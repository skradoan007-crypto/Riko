# P8 follow-up: MP3 PCM timestamp correction

## Device evidence and scope

The supplied 2026-11-13 screenshot for FUNK POWER (Ultra Slowed) confirms
TIMESTAMP, which previously combined an initial offset and a later discontinuity.
It does not identify the file codec, the decoder implementation or the actual
numeric offset. The second screenshot for FUNK TAKA (SLOWED) shows an in-app
45 FPS / 21.9 ms sample during analysis. It is not a sustained performance trace.

## Reproduced defect

The analyzer requires continuous PCM timestamps within 2 ms of sample-count time.
AOSP's legacy OMX MP3 decoder trims 529 PCM frames from its first output but its
following timestamps still include those frames. C2 timestamps using frames
actually returned instead. The old shared source forwarded both conventions
without correction, so the legacy convention failed valid-stream analysis.

At 44.1 kHz, a first buffer of 623 frames (1152 - 529) implies a second-buffer
start at 14,126 us. The legacy timestamp is 26,122 us: a 11,996 us mismatch.
Subtracting the integer 529-frame duration (11,995 us) leaves 1 us rounding.

Primary references:
- https://android.googlesource.com/platform/frameworks/av/+/a22302ec9a1fd4cbeb06c8e3a9d36a54805d8f21/media/libstagefright/codecs/mp3dec/SoftMP3.cpp
- https://android.googlesource.com/platform/frameworks/av/+/refs/heads/main/media/codec2/components/mp3/C2SoftMp3Dec.cpp
- https://github.com/google/ExoPlayer/issues/8594

## Implementation

- Mp3PcmTimestampNormalizer runs in the existing AndroidPcmSource delivery path,
  shared by waveform and DNA. It applies only to audio/mpeg and only when both
  the first output's frame count and the second output timestamp independently
  match the 529-frame trim signature. MPEG-1 (1152) and MPEG-2/2.5 (576) frames,
  batched first outputs and one-sample timestamp rounding are handled.
- The correction is selected once, without using the codec's name. Empty output
  does not advance it. C2 and non-MP3 sources receive no correction. No PCM is
  discarded or synthesized; no decode loop is copied.
- The existing analyzer's 2 ms guard remains. Subsequent dropped audio, changed
  format, unrelated gaps and missing initial audio still fail. Duration/EOS,
  cancellation and version-1 golden compatibility remain checked.
- DnaTimestampException carries only numeric sample rate, frames consumed,
  expected time and received time. Analyzer/repository retain these values and
  the scrollable error dialog displays them if another timing case still fails.
  No file paths, URIs, raw codec exception messages or device identifiers appear.

## Validation actually run

- tools/test_genesis_analyzer.sh: 17 JVM contract groups PASS, -Xlint/-Werror.
  The former legacy-OMX failure is reproduced without normalization. Corrected
  legacy and C2 traces yield identical fingerprints, features and every timeline
  value at 8, 16, 22.05, 24, 32, 44.1 and 48 kHz, including batched first outputs.
  Unrelated gaps, wrong MIME, untrimmed buffers, empty output, invalid timestamps
  and later data loss remain covered. Existing V1 goldens PASS.
- tools/test_genesis_final.sh: camera, DNA code/timeline and spectrum contracts PASS.
- Two synthetic MP3 fixtures (16 kHz and 44.1 kHz stereo) decode successfully
  with host FFmpeg. They are generated from the existing waveform-sections.wav:
  ffmpeg -i waveform-sections.wav -map_metadata -1 -c:a libmp3lame -b:a 128k waveform-sections.mp3
  ffmpeg -i waveform-sections.wav -map_metadata -1 -ar 44100 -c:a libmp3lame -b:a 128k waveform-sections-44100.mp3
- Android instrumented waveform/DNA tests now include both MP3 fixtures alongside
  WAV/AAC; repository tests assert numeric diagnostic propagation. These Android
  tests are written, NOT run here. No local Android build or new CI evidence.
- Diff whitespace, changed-file credential scan, ZIP integrity and delivery patch
  reconstruction are checked before packaging.

## Remaining acceptance

The known valid-MP3 failure is fixed and reproduced on the host. The user's exact
file/device path is not yet reproduced, so this is not a claim that every
TIMESTAMP failure is resolved. Re-test the supplied song on Android; if it still
fails, the numeric diagnostic distinguishes the next case without guessing.
No renderer changes were made in this follow-up. The 45 FPS observation remains
unresolved; no new on-device frame, battery, thermal or playback-sync measurements
are available. This source is not a verified APK or proof of the desired visual result.
