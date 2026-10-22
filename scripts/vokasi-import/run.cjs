'use strict';
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const zlib = require('node:zlib');
const { createClient } = require('@supabase/supabase-js');

const EXPECTED = 2378;
const BATCH_SIZE = 25; // Avoid Edge Function request timeouts; not a limit on total students.
const ALLOWED_MAJORS = new Set(['DPIB','GEO','TAB','TITL','TKP','TKR','TMI','TOI','TP','TPM','TSM']);
const MODE = process.env.VOKASI_IMPORT_MODE || 'preview';
const RESUME = process.env.VOKASI_IMPORT_RESUME === 'true';
const CONFIRM = process.env.VOKASI_IMPORT_CONFIRM || '';

function fail(msg) { throw new Error(msg); }
function decode() {
  const hex = (process.env.VOKASI_IMPORT_AES_KEY || '').trim();
  if (!/^[0-9a-f]{64}$/i.test(hex)) fail('GitHub Secret VOKASI_IMPORT_AES_KEY belum diatur atau tidak valid.');
  const file = path.join(__dirname, 'roster-2026-2027.enc.json');
  if (!fs.existsSync(file)) fail('File roster terenkripsi belum tersedia di repository.');
  const doc = JSON.parse(fs.readFileSync(file, 'utf8'));
  if (doc.version !== 1 || doc.format !== 'aes-256-gcm+gzip' || doc.students !== EXPECTED) fail('Metadata file roster tidak valid.');
  const encrypted = Buffer.from(doc.ciphertext, 'base64');
  if (encrypted.length < 17) fail('File roster terenkripsi rusak.');
  const decipher = crypto.createDecipheriv('aes-256-gcm', Buffer.from(hex,'hex'), Buffer.from(doc.nonce,'base64'));
  decipher.setAAD(Buffer.from('VOKASI_ROSTER_2026_2027'));
  decipher.setAuthTag(encrypted.subarray(encrypted.length - 16));
  const zipped = Buffer.concat([decipher.update(encrypted.subarray(0,-16)), decipher.final()]);
  const raw = zlib.gunzipSync(zipped);
  if (crypto.createHash('sha256').update(raw).digest('hex') !== doc.sha256) fail('Checksum daftar siswa berbeda; impor dibatalkan.');
  const data = JSON.parse(raw.toString('utf8'));
  if (data.version !== 1 || data.schoolYear !== '2026/2027' || !Array.isArray(data.students) || data.students.length !== EXPECTED) {
    fail('Isi daftar siswa tidak sesuai data sumber.');
  }
  const unique = new Set();
  for (const [index, s] of data.students.entries()) {
    const fullName = String(s.fullName || '').trim();
    const major = String(s.majorCode || '').trim();
    const label = String(s.classLabel || '').trim();
    const year = Number(s.admissionYear);
    const grade = Number(s.gradeLevel);
    if (fullName.length < 2 || fullName.length > 120 || !ALLOWED_MAJORS.has(major) ||
        !/^(XII|XI|X)\s+[A-Za-z0-9 .-]+$/.test(label) || ![10,11,12].includes(grade) ||
        !Number.isInteger(year) || year < 2000 || year > 2100 || s.schoolYear !== '2026/2027') {
      fail(`Baris ${index+1} tidak valid; impor dibatalkan.`);
    }
    const gradeLabel = label.match(/^(XII|XI|X)\s+/)[1];
    const expectedGrade = gradeLabel === 'X' ? 10 : gradeLabel === 'XI' ? 11 : 12;
    if (expectedGrade !== grade) fail(`Tingkat tidak sesuai pada baris ${index+1}.`);
    const key = `${fullName.replace(/\s+/g,' ').toLocaleLowerCase('id-ID')}|${major.toLowerCase()}|${year}`;
    if (unique.has(key)) fail(`Identitas ganda pada baris ${index+1}.`);
    unique.add(key);
  }
  return data.students;
}

function summary(body) {
  if (!process.env.GITHUB_STEP_SUMMARY) return;
  fs.appendFileSync(process.env.GITHUB_STEP_SUMMARY, body+'\n');
}
async function main() {
  if (!['preview','import'].includes(MODE)) fail('Mode harus preview atau import.');
  if (MODE === 'import' && CONFIRM !== 'IMPORT_2378_VOKASI') fail('Konfirmasi import tidak sesuai.');
  const students = decode();
  console.log(`Roster terenkripsi valid: ${students.length} siswa; 11 jurusan.`);
  const URL = process.env.VOKASI_SUPABASE_URL;
  const KEY = process.env.VOKASI_SUPABASE_ANON_KEY;
  const EMAIL = process.env.VOKASI_ADMIN_EMAIL;
  const PASSWORD = process.env.VOKASI_ADMIN_PASSWORD;
  if (!URL || !KEY || !EMAIL || !PASSWORD) fail('Konfigurasi URL, publishable key, atau secret admin belum lengkap.');
  const sb = createClient(URL, KEY, {auth:{persistSession:false,autoRefreshToken:false}});
  async function login() {
    const {data,error} = await sb.auth.signInWithPassword({email:EMAIL,password:PASSWORD});
    if (error || !data?.user) fail('Login admin Supabase gagal; periksa email/password secret.');
    // The authenticated role intentionally cannot SELECT school_admins.
    // Use the security-definer RPC already granted to authenticated users.
    const {data:isAdmin,error:adminError} = await sb.rpc('is_school_admin');
    if (adminError) fail('Pemeriksaan izin admin gagal: '+adminError.message);
    if (isAdmin !== true) fail('Akun masuk bukan admin sekolah aktif; periksa akun admin yang disimpan di GitHub Secrets.');
    return data.user.id;
  }
  await login();
  const {count,error} = await sb.from('student_roster').select('id',{count:'exact',head:true});
  if (error || count == null) fail('Tidak dapat memeriksa jumlah roster di server.');
  console.log(`Jumlah roster di Supabase sebelum run: ${count}.`);
  if (MODE === 'import' && !RESUME && count !== 0) fail('Roster tidak kosong. Untuk melanjutkan impor terhenti pilih Resume; jangan reset data.');
  if (MODE === 'preview') {
    console.log('PREVIEW BERHASIL. Tidak ada akun siswa dibuat atau diubah.');
    summary(`### VOKASI – Pratinjau\n- File valid: ${students.length} siswa\n- Roster server saat ini: ${count}\n- Data tidak diubah.`);
    return;
  }
  let created=0,existing=0,batches=0;
  const attemptsMax=3;
  for (let off=0; off<students.length;off+=BATCH_SIZE) {
    const batch = students.slice(off,off+BATCH_SIZE);
    let result;
    for (let attempt=1;attempt<=attemptsMax;attempt++) {
      try {
        // Refresh credentials periodically to avoid expired JWT in long jobs.
        if (off>0 && off%(BATCH_SIZE*12)===0 && attempt===1) await login();
        const {data,error} = await sb.functions.invoke('provision-students',{body:{students:batch}});
        if (error) throw error;
        result = typeof data === 'string' ? JSON.parse(data) : data;
        if (!result || result.failed || result.success !== true ||
          Number(result.created||0) + Number(result.existing||0) !== batch.length) {
          const failure=result?.failures?.[0];
          fail(`Batch ${Math.floor(off/BATCH_SIZE)+1} ditolak. Gagal=${result?.failed ?? '?'}; `+
               `baris batch=${failure?.row ?? '?'}; alasan=${failure?.error ?? 'hasil tidak sesuai'}.`);
        }
        break;
      } catch(e) {
        const msg = String(e?.message || e);
        // Explicit row failures are not transient; do not repeat automatically.
        if (msg.includes('ditolak.')) throw e;
        if (attempt===attemptsMax) fail(`Batch ${Math.floor(off/BATCH_SIZE)+1} gagal setelah retry: ${msg.slice(0,200)}. Gunakan Resume sesudah diperbaiki.`);
        console.log(`Batch ${Math.floor(off/BATCH_SIZE)+1}: kendala koneksi (percobaan ${attempt}/${attemptsMax}); coba lagi.`);
        await new Promise(resolve=>setTimeout(resolve,attempt*3000));
        await login();
      }
    }
    created+=Number(result.created || 0);
    existing+=Number(result.existing || 0);
    batches++;
    console.log(`Batch ${batches}/${Math.ceil(students.length/BATCH_SIZE)} selesai. Proses ${Math.min(off+BATCH_SIZE,students.length)}/${students.length}.`);
  }
  const {count:finalCount,error:finalError} = await sb.from('student_roster').select('id',{head:true,count:'exact'});
  if (finalError || finalCount !== EXPECTED) fail(`Semua batch diproses tetapi jumlah roster akhir ${finalCount ?? '?'} bukan ${EXPECTED}. Periksa data; jangan jalankan ulang tanpa audit.`);
  console.log(`IMPOR SELESAI: ${EXPECTED} roster, ${created} akun baru, ${existing} akun yang sudah ada.`);
  summary(`### VOKASI – Hasil impor\n- Roster terverifikasi: ${finalCount}/${EXPECTED}\n- Akun dibuat: ${created}\n- Akun sudah ada: ${existing}\n- Batch: ${batches}\n- Data admin dan master sekolah tidak dihapus.`);
}
main().catch(err=>{console.error('VOKASI IMPORT ERROR:',err.message||String(err));process.exitCode=1;});