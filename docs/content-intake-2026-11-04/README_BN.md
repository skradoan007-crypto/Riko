# ৪ নভেম্বরের কনটেন্ট গ্রহণ ও তুলনা

এটি app পরিবর্তনের রিপোর্ট নয়। এই ধাপে source, image ZIP ও নতুন code তালিকা মিলিয়ে তথ্য সংরক্ষণ করা হয়েছে। Android build চালানো হয়নি।

- পাঁচটি ZIP-এ মোট ২৭৯টি image entry: ৬৪ + ৬৫ + ৫ + ৫৯ + ৮৬। প্রতিটি ZIP-এর CRC, manifest SHA-256 ও image decoding পরীক্ষায় উত্তীর্ণ। পুনরাবৃত্ত item/image আছে; এটি unique cheat count নয়।
- ব্যবহারকারীর নতুন তালিকায় ১৪০টি আলাদা string code। সব ১৪০টি code বর্তমান catalog-এ পাওয়া যায়। নামের মিল বা কার্যকারিতার নিশ্চিতকরণ এর দ্বারা বোঝায় না। প্রতিটির তুলনা code-comparison.json-এ আছে।
- ব্যবহারকারী তালিকাটিকে official বলেছেন; প্রকাশকের URL দেওয়া হয়নি। provenance সেইভাবেই রাখা হয়েছে।
- নতুন HTML-এ ২৪টি RGS Load File, প্রত্যেকটির .glb URL ও thumbnail path সংরক্ষিত। thumbnail batch এখনও ব্যবহারকারীর কাছ থেকে আসেনি।

## প্রধান পার্থক্য

| Code | নতুন তালিকা | আগের সমস্যা |
|---|---|---|
| 4444 | Cobra | app-এ Bugatti Chiron; ব্যবহারকারীর gameplay-তে Spawn Cobra Bike দেখা যায় |
| 500 | Bugatti | app-এ Audi এবং Bugatti conflict—দুটি entry |
| 800 | Bugatti v2 | আগের HTML-এ Audi |
| 6 | Buffalo | আগের HTML-এ Elephant |
| 6999 | Elephant | app-এ alternative; আগের HTML-এ এই code নেই |
| 6666 | Range Rover | app/HTML-এ G-Wagon-ও আছে |
| 9000 | Flying Bike | app/HTML-এ Splendor/Super Splendor-ও আছে |
| 6021 | Super Splendor | আগের HTML-এ Splendor |
| 7112 | Moon Gravity | আগের HTML-এ Ferrari-ও আছে |
| npdance | RGS Tool Dance | RGS HTML-এ npcdance; এক অক্ষরের পার্থক্য সংরক্ষিত |

নতুন তালিকাতেও শ্রেণিবিভাগে অসংগতি আছে: Cobra-কে Cars & SUVs এবং Legendar-কে Items বলা হয়েছে। app-এর filter-এর জন্য এগুলো যথাক্রমে Bike ও Car হবে। Building b6/b7-এর title শুধু Building; c1-এর title p10। এগুলো code বদলানোর কারণ নয়।

আগের HTML batch-এ অনুপস্থিত কিন্তু নতুন তালিকায় থাকা code: 53, 6999, 0002, 101, npdance, 12345, 54321, 9, 002, 1212, 01212, pm9। এগুলো app-এ আগে থেকেই আছে।

## নতুন বিভাগ

Cheat Codes, RGS Tools, Plugins, RGS Load Files এবং Custom RGS File Loader আলাদা উদ্দেশ্যের বিভাগ।

RGS Load Files: তালিকা, স্থানীয় thumbnail, আসল download URL। Custom RGS File Loader-এর আলাদা source দেওয়া হয়নি; প্রস্তাবিত কাজ ফোন থেকে GLB নির্বাচন, structure validation, সংরক্ষণ ও export/share। গেমে সরাসরি import সমর্থন পরীক্ষা হয়নি।

Loader HTML-এর একটি MotuPatlu NPC কার্ডে franklin.jpg এবং franklin.glb রয়েছে। source title ও URL অপরিবর্তিত সংরক্ষণ করা হয়েছে; thumbnail দেখা/ফাইল পরীক্ষা ছাড়া পরিচয় নিশ্চিত বলা হয়নি।
