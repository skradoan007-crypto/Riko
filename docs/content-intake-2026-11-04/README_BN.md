# IBD3D Panel — বাংলা audit, ৫ নভেম্বর ২০২৬

Source-এ implementation করা হয়েছে। Android build/emulator/gameplay test চালানো হয়নি; APK বা compile success দাবি করা হচ্ছে না।

| অংশ | Browseable records |
|---|---:|
| Cheat Codes | 155 |
| RGS Tools | 65 |
| Plugins / URL | 86 |
| Offline RGS Load Files | 23 |
| মোট browseable | 329 |
| Archived | 17 |
| মোট catalog | 346 |

মূল ২১২টি ID ও exact code string রাখা হয়েছে। ১৩৪টি নতুন record আছে—সব নতুন phone cheat নয়; URL/model/RGS/weather stage-ও আছে। ১২টি একই-code ভুল/পুরোনো label archived করে favorites/history canonical ID-তে migrate হয়। আগের অন্য game-এর ৫টি দাবি browsing-এ নেই।

প্রত্যেক মূল entry-এর before/after: [ALL_ORIGINAL_ENTRIES.md](ALL_ORIGINAL_ENTRIES.md), source/notes-সহ JSON: [all-original-entries.json](all-original-entries.json)। নতুন records: [NEW_ENTRIES.md](NEW_ENTRIES.md)। এটি প্রতিটি row-এর content reconciliation; সব code নতুন করে game-এ পরীক্ষার দাবি নয়। Supplied source ব্যবহারকারীর নির্দেশ অনুযায়ী গ্রহণ করা হয়েছে; independent official evidence হিসেবে সাজানো হয়নি।

## প্রধান সংশোধন

| Code | বর্তমান নাম | সংশোধন |
|---|---|---|
| 4444 | Cobra Bike · Bike | Bugatti Chiron বাদ; user gameplay evidence আছে |
| 500 | Bugatti | Audi duplicate archived |
| 800 | Bugatti v2 | পুরোনো HTML-এর Audi ব্যবহার হয়নি |
| 6 | Buffalo | Elephant-এর ভুল association ঠিক |
| 6999 | Elephant | সঠিক supplied thumbnail |
| 6666 | Range Rover | G-Wagon duplicate archived; placeholder |
| 9000 | Flying Bike | Splendor duplicate archived |
| 6021 | Super Splendor | নতুন তালিকার label |
| 7112 | Moon Gravity | Ferrari mapping বাদ |
| npdance | RGS Tool Dance | নতুন তালিকার spelling |
| 99 | ৫ weather stage + Time Change | শুধু বানান-ভিন্ন duplicate merge; repeat-call notes |
| 04444 | Unconfirmed monster variant | ভুল Bugatti Chiron base থেকে তৈরি দাবি conflict |

Leading zero ও alphanumeric code অপরিবর্তিত। পুরোনো experimental/unverified record-কে confirmed বানানো হয়নি।

## ছবি ও model

পাঁচ image ZIP-এর ২৭৯টি source entry manifest SHA/CRC/decode মিলিয়ে নেওয়া হয়েছে। Model pack থেকে ২৩টি model ও image যোগ হয়েছে; **Gym Props বাদ**। Model-এর .glb নাম থাকলেও ভেতরে **UnityFS AssetBundle**, glTF mesh নয়। মূল bytes রাখা হয়েছে; প্রতিটির header size ও SHA পাস। Game compatibility এখনো পরীক্ষা হয়নি।

মোট model payload 182,747,780 bytes (~174 MiB)। GTA 5 City বড় হওয়ায় Git-এর জন্য segment করা হয়েছে; app চাওয়ার সময় cache-এ জোড়া লাগায়। Startup-এ model load/render হয় না। APK/AAB size মাপা হয়নি।

৪৪টি active entry-তে ছবি নেই; placeholder থাকবে। আরও ২টি illustrative ছবি: More Traffic Car (54321), Night Mode (9)। Duke (4215) ও Duke 200 (7777) একই bytes ব্যবহার করে; supplied mapping রাখা হয়েছে, exact variant association পুনরায় দেখা দরকার। প্রত্যেক নাম/code/ছবির প্রয়োজন: [IMAGE_ISSUES.md](IMAGE_ISSUES.md)।

## নতুন আচরণ

- আগের light purple-blue design/icon/name/package রাখা হয়েছে। Plugins আর Coming Soon নয়; চারটি resource section আছে।
- First-launch resource notice: ৭ visible second পরে X; acknowledgement স্থায়ী। About-এ একই notice ও policy।
- Copy reminder: in-app notification নয়। Last copy-এর ১০ সেকেন্ডের মধ্যে background হলে নাম/code এবং You Just Copied. Use It; একই notification ২ সেকেন্ডে update, ৪ সেকেন্ডে silent update, ৭ সেকেন্ডে remove। Return/dismiss respected; permission ও setting আছে। Android popup duration/DND/cooldown নিয়ন্ত্রণ করে।
- Load compatible game file handler থাকলে read-only URI পাঠায়; না থাকলে RGS picker/Save file। SAF ব্যবহার করলে IBD3D Panel provider পাওয়া যাবে। Minimized game-এর UI নিজে নিজে চাপার verified API নেই; guaranteed automatic import করা হয়নি।
- Custom loader: GLB2/UnityFS বাছাই, private copy, 256 MiB limit, header check, save/remove। Header check compatibility proof নয়।
- Get the Game আগের store-only route; game launch করে না।

## পরীক্ষা

26 Python tests পাস: historical baseline/current IDs/codes/aliases/sections/images/model bytes/rights coverage/XML। 32 Kotlin/Kotlin-DSL file formatter-এ parse পাস; compilation/type-check নয়। 7 XML parse পাস। 38 Kotlin/JUnit/UI test দেওয়া আছে, এই revision-এ চালানো হয়নি। [data-test-results.txt](data-test-results.txt), [kotlin-format-results.txt](kotlin-format-results.txt), [DEVICE_CHECKS.md](DEVICE_CHECKS.md)। FPS/startup/physical-device/Play/game-import ফল দাবি নেই।

## Release-এর আগে যা প্রয়োজন

Support/privacy/rights email, public policy URL ও third-party resources পুনর্বিতরণের permission/license। Disclaimer অনুমতি তৈরি করে না। [Policy প্রকাশের বাংলা ধাপ](../legal/PUBLISHING_BN.md)।
