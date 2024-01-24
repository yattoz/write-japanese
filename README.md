# write-japanese

Write Japanese is an Android app that teaches the Japanese character set, including katakana, hiragana, and over 3000 kanji characters. It features animated demonstrations, realtime stroke order evaluation, detailed feedback, customizable character lists, kanji stories as a memorization aid, and a Spaced Repetition System (SRS) that schedules learning repeats over several days. It has been on sale in the Google App store for several years, and was open-sourced under the GPL in 2023.

It is recommended to build using the free Android Studio IDE.

For screenshots and info, see the [Google Play Store listing](https://play.google.com/store/apps/details?id=dmeeuwis.kanjimaster&hl=en_U).

# Build notes

If you're using Windows, chances are Git is automatically checking out text files with line ends CRLF. 
**This causes an issue with the dictionary file kanjidic.utf8.awb that is read like you would with a binary file.**
**Be sure to save kanjidic.utf8.awb as LF (and not CRLF) after checking out the repository.**