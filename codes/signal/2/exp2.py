import numpy as np
import librosa
import soundfile as sf
from scipy.fft import fft, ifft

original_t = list()
original_f = list()
filtered_f = list()

for i in range(4):
    tmp_t, sr = librosa.load(f"./data/src.wav", sr=8000, offset=i*30, duration=30)
    original_t.append(tmp_t)
    tmp_f = fft(tmp_t)
    original_f.append(tmp_f)
    for j in range(102000, 138000):
        tmp_f[i] = 0
    filtered_f.append(tmp_f)
    sf.write(f"./inter/{i}.wav", np.real(tmp_t), sr, format="WAV")

mixed_f = np.hstack((filtered_f[0][0:120000], filtered_f[1][0:120000], filtered_f[2][0:120000], filtered_f[3][0:120000],
                     filtered_f[3][120000:240000], filtered_f[2][120000:240000],
                     filtered_f[1][120000:240000], filtered_f[0][120000:240000]))
mixed_t = ifft(mixed_f)
sf.write(f"./output/mixed.wav", np.real(mixed_t), 48000, format="WAV")

decoded_f = list()
decoded_t = list()

mixed_f = fft(mixed_t)
for i in range(4):
    tmp_f = np.hstack((mixed_f[120000 * i:120000 * (i + 1)], mixed_f[120000 * (7 - i):120000 * (8 - i)]))
    decoded_f.append(tmp_f)
    tmp_t = ifft(tmp_f)
    decoded_t.append(tmp_t)
    sf.write(f"./output/decoded_{i}.wav", np.real(tmp_t), sr, format="WAV")
