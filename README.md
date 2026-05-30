# 🗳️ স্মার্ট ভোটার অনুসন্ধান অ্যাপ (Offline Smart Voter Search)

এটি একটি সম্পূর্ণ অফলাইন এবং দ্রুতগতির অ্যান্ড্রয়েড অ্যাপ্লিকেশন, যার মাধ্যমে খুব সহজেই নির্দিষ্ট এলাকার ভোটারদের তথ্য (নাম, ভোটার নম্বর, ঠিকানা ইত্যাদি) খুঁজে বের করা যায়। হাজার হাজার ভোটারের তথ্য থেকে চোখের পলকে সঠিক ডাটা বের করার জন্য এটি একটি দারুণ টুল!

## ✨ মূল ফিচারসমূহ (Key Features)
* **১০০% অফলাইন:** অ্যাপটি চালাতে কোনো ইন্টারনেট সংযোগের প্রয়োজন নেই।
* **সুপার ফাস্ট সার্চ:** SQLite ডাটাবেস ব্যবহারের কারণে বিশাল ডাটাবেস থেকেও চোখের পলকে রেজাল্ট চলে আসে।
* **স্মার্ট ফিল্টারিং:** খুব সহজেই **ওয়ার্ড নম্বর** এবং **লিঙ্গ (পুরুষ/মহিলা)** অনুযায়ী ফিল্টার করে খোঁজার সুবিধা।
* **সহজ ইউজার ইন্টারফেস (UI):** সাধারণ মানুষের ব্যবহারের উপযোগী করে একদম পরিষ্কার ও সুন্দর ডিজাইন করা হয়েছে। 
* **বিস্তারিত ফলাফল:** প্রতিটি রেজাল্টের সাথে ভোটার কোন ফোল্ডারের বা কত নম্বর পেজের সেটা ব্যাজ (Badge) আকারে দেখায়।

## 🛠️ ব্যবহৃত প্রযুক্তি (Tech Stack)
* **ভাষা:** Kotlin (Android)
* **ডাটাবেস:** SQLite (Assets ফোল্ডার থেকে লোকাল ডাটাবেস কপি করার সিস্টেম)
* **ইউজার ইন্টারফেস:** XML (Android Studio)

## 📱 সাধারণ ব্যবহারকারীদের জন্য (কিভাবে ইন্সটল করবেন)
আপনি যদি শুধু অ্যাপটি আপনার ফোনে ব্যবহার করতে চান, তবে নিচের ধাপগুলো অনুসরণ করুন:
১. এই পেজের ডানদিকে **[Releases](https://release-assets.githubusercontent.com/github-production-release-asset/1253382533/ec3ab443-91c9-4aca-8714-87a218bad9aa?sp=r&sv=2018-11-09&sr=b&spr=https&se=2026-05-30T17%3A00%3A49Z&rscd=attachment%3B+filename%3Dapp-debug.apk&rsct=application%2Fvnd.android.package-archive&skoid=96c2d410-5711-43a1-aedd-ab1947aa7ab0&sktid=398a6654-997b-47e9-b12b-9515b896b4de&skt=2026-05-30T16%3A00%3A03Z&ske=2026-05-30T17%3A00%3A49Z&sks=b&skv=2018-11-09&sig=9nuT1jXArqdKZAvptlLn98ersGb0ulxXuGw5dTHhAoA%3D&jwt=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmVsZWFzZS1hc3NldHMuZ2l0aHVidXNlcmNvbnRlbnQuY29tIiwia2V5Ijoia2V5MSIsImV4cCI6MTc4MDE1ODYzMywibmJmIjoxNzgwMTU2ODMzLCJwYXRoIjoicmVsZWFzZWFzc2V0cHJvZHVjdGlvbi5ibG9iLmNvcmUud2luZG93cy5uZXQifQ.IPhFEwCFZI2arhdDU6sKniKzJb_zlBnpbDq8UxT2GU8&response-content-disposition=attachment%3B%20filename%3Dapp-debug.apk&response-content-type=application%2Fvnd.android.package-archive)** সেকশনে যান।
২. সেখান থেকে সর্বশেষ ভার্সনের `.apk` ফাইলটি (যেমন: `app-debug.apk`) ডাউনলোড করুন।
৩. আপনার ফোনে ফাইলটি ওপেন করে ইন্সটল করুন। 
   *(বিঃদ্রঃ যেহেতু অ্যাপটি প্লে-স্টোরে নেই, তাই ইন্সটলের সময় Google Play Protect একটি ওয়ার্নিং দিতে পারে। সেক্ষেত্রে **"More details"** এ ক্লিক করে **"Install anyway"** সিলেক্ট করুন।)*

## 💻 ডেভেলপারদের জন্য (কিভাবে কোড রান করবেন)
আপনি যদি এই প্রোজেক্টটি নিয়ে কাজ করতে চান বা আরও উন্নত করতে চান, তবে নিচের ধাপগুলো অনুসরণ করুন:

**ধাপ ১:** প্রজেক্টটি ক্লোন (Clone) করুন:
```bash
git clone [https://github.com/আপন](https://github.com/আপন)ার-ইউজারনেম/আপনার-রিপোজিটরির-নাম.git
