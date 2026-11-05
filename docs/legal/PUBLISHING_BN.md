# Policy প্রকাশ ও resource-এর অধিকার

First-launch notice, About-এর notice এবং offline policy app-এ বসানো হয়েছে। এগুলো copyright permission বা Play approval-এর নিশ্চয়তা নয়। “অন্য website থেকে সংগ্রহ করেছি” বলা credit; পুনর্বিতরণের license আলাদা বিষয়।

প্রস্তুত আছে: app-এর `core/data/src/main/assets/policies.txt`, ওয়েব policy খসড়া `docs/legal/privacy.html` এবং source/SHA/license ledger `docs/content-intake-2026-11-04/asset-rights-register.json`। অনুমতির প্রমাণ না পাওয়ায় ledger-এ `permission_not_supplied` লেখা আছে।

## প্রকাশের ধাপ

1. একটি কার্যকর support/privacy/rights email ঠিক কর। HTML-এর `[SUPPORT_EMAIL]` এবং app policy-তে সেটি বসাও; একই contact Play Console-এ দাও। এখনো কোনো ঠিকানা অনুমান করে বসানো হয়নি।
2. প্রতিটি third-party image/model-এর license বা লিখিত permission সংগ্রহ কর। Download করা যায় মানেই redistribution-এর অনুমতি নয়। Game চরিত্র, গাড়ির artwork, branding এবং GTA/অন্যান্য asset-এর অধিকারও আলাদা করে দেখ। অনুমতি না থাকলে public release থেকে সরাও বা নিজের/লাইসেন্সকৃত বিকল্প দাও।
3. খসড়ার সঙ্গে বাস্তব app/dependencies মিলিয়ে নে। এই source-এ accounts/ads/analytics/server upload নেই। Favorites/history/preferences/imported files ফোনে থাকে; external links, Google Play এবং game-এর plugin downloads পৃথক service।
4. সম্পাদিত HTML নিজের HTTPS website বা GitHub Pages-এ প্রকাশ কর। Login ছাড়া, সব দেশ থেকে দেখা যায় এমন স্থায়ী webpage দরকার; ZIP/PDF/raw download নয়। GitHub Pages হলে Settings → Pages থেকে branch/folder সেট করে প্রকাশিত URL খুলে দেখ।
5. Play Console → Policy and programs → App content → Privacy Policy-তে ওই URL দে। Data safety form বাস্তব app/SDK অনুযায়ী পূরণ কর; শুধু offline শুনে সব No দিস না। About-এ offline text আছে; URL ঠিক হলে সেটিও যোগ করা যাবে।
6. Rights complaint পেলে resource/source, দাবিদারের অধিকার ও যোগাযোগ, চাওয়া correction/removal সংগ্রহ করে review কর। প্রয়োজনে বিতর্কিত content সরিয়ে যাচাই কর। এই খসড়া কোনো নির্দিষ্ট দেশের statutory notice process সম্পন্ন হওয়ার দাবি নয়।
7. Release-এর আগে প্রাসঙ্গিক আইনি পরামর্শ নিয়ে permission/ownership যাচাই কর। Engineering draft legal clearance নয়।

## এখনো যা প্রয়োজন

- কার্যকর support/privacy/rights email এবং চূড়ান্ত policy URL।
- Third-party assets পুনর্বিতরণের license/permission।
- Automatic game import চাইলে game-এর documented API/intent contract। শুধু file URI পাঠানো মানেই import success নয়।

## Official references

- Intellectual property: https://support.google.com/googleplay/android-developer/answer/9888072?hl=en
- User data: https://support.google.com/googleplay/android-developer/answer/10144311?hl=en
- Privacy URL: https://support.google.com/googleplay/android-developer/answer/9859455?hl=en
- Notification cooldown: https://developer.android.com/develop/ui/compose/notifications

পরের উপকারী উন্নতি: বাংলা/হিন্দি localization, item-এর game-version/শেষ যাচাইয়ের তারিখ, offline favorites backup/import এবং বাস্তব device benchmark/baseline profiles। Ranking নিশ্চিত করার কোনো feature নেই; আগে accuracy, performance ও resource permission ঠিক রাখাই জরুরি। এগুলো অতিরিক্ত feature হিসেবে এই revision-এ যোগ করা হয়নি।
