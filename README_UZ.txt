==========================================
   CalcNote — Qatorma-qator hisoblagich
==========================================

Bu nima?
--------
CalcNote'ga o'xshash dastur. Har bir qatorga ifoda yozasiz
(masalan 2360 * 16), o'ng tomonda natija chiqadi, pastda esa
hamma qatorlar yig'indisi "Jami" ko'rinadi.

Asosiy imkoniyatlar:
  - Qatorma-qator hisob, o'ngda natija, pastda JAMI
  - Qo'shish/ayirish/ko'paytirish ANIQ (BigDecimal — kasrli xato yo'q)
  - Amallar:  +  -  ×  ÷  mod  ( )  Ans  %
  - Ma'lumot AVTOMATIK saqlanadi (dastur yopilsa ham yo'qolmaydi)
  - Menyu: Yangi fayl, Saqlash, Fayllar, Nomini o'zgartirish,
           Ulashish, Undo/Redo, Tozalash
  - Ko'p fayl: bir nechta alohida hisob varaqasi
  - O'z klaviaturasi (telefon klaviaturasi chiqmaydi)
  - Internet ruxsati YO'Q — ma'lumot telefondan chiqmaydi


=====================================================
   APK QANDAY YIG'ILADI (GitHub Actions orqali)
=====================================================
Sizda kompyuterda Android muhiti shart emas — hammasi
GitHub'da yig'iladi.

1-qadam. GitHub'da YANGI repository oching
   - github.com -> New repository
   - Nomi masalan: CalcNote
   - Public yoki Private — farqi yo'q
   - "Create repository" bosing

2-qadam. Shu ZIP ichidagi fayllarni repository'ga yuklang
   - ZIP'ni oching, ichidagi HAMMA narsani (papkalari bilan)
     repository'ga "Add file -> Upload files" orqali tashlang.
   - DIQQAT: .github papkasi ham yuklanishi shart
     (uning ichida workflows/build.yml bor — APK shu bilan yig'iladi).
   - "Commit changes" bosing.

3-qadam. Yig'ilishini kuting
   - Repository'da yuqoridagi "Actions" bo'limiga o'ting.
   - "Build CalcNote APK" ishga tushadi (yashil belgi = tayyor).
   - Agar o'zi boshlanmasa: Actions -> Build CalcNote APK ->
     "Run workflow" tugmasini bosing.

4-qadam. APK'ni yuklab oling
   - Tugagan ishni oching (yashil belgili).
   - Pastda "Artifacts" bo'limidan "CalcNote-debug-apk" ni bosing.
   - ZIP tushadi, ichida: app-debug.apk


=====================================
   TELEFONGA O'RNATISH
=====================================
1. app-debug.apk ni telefonga ko'chiring.
2. Faylni bosib oching.
3. "Noma'lum manbalardan o'rnatish" so'rasa — ruxsat bering.
4. O'rnatish -> Ochish.


=====================================
   ESLATMA (sozlash kerak bo'lsa)
=====================================
- Dastur nomi/ikonkasini keyin o'zgartirsa bo'ladi.
- "release" (Play Market uchun imzolangan) versiya kerak bo'lsa,
  alohida sozlash (keystore) kerak — ayting, qo'shib beraman.
- Yangi amal yoki funksiya kerak bo'lsa yozing.
