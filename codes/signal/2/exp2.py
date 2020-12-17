import numpy as np
import librosa
import soundfile as sf
import matplotlib.pyplot as plt
from scipy.fft import fft, ifft
from sklearn.metrics import mean_squared_error

LENGTH = 30
SAMPLE_RATE = 8000
FILTER_FREQ = 3400
NUM_POINT = LENGTH * SAMPLE_RATE
FILTER_POINT = LENGTH * FILTER_FREQ

t = np.linspace(0, 30, NUM_POINT)
f = np.linspace(0, 2, NUM_POINT)


def plot_time(data, title="", filename=None):
    plt.figure()
    plt.plot(t, data)
    plt.title(title)
    plt.savefig("./fig/{}.png".format(filename if filename is not None else "time"))
    # plt.show()
    plt.close()


def plot_freq(data, title="", filename=None):
    plt.figure()
    plt.plot(f, data)
    plt.title(title)
    plt.savefig("./fig/{}.png".format(filename if filename is not None else "freq"))
    # plt.show()
    plt.close()


original_t = list()
original_f = list()
filtered_t = list()
filtered_f = list()

for i in range(4):
    tmp_t, sr = librosa.load(f"./data/src.wav", sr=SAMPLE_RATE, offset=i * LENGTH, duration=LENGTH)
    original_t.append(tmp_t.copy())
    tmp_f = fft(tmp_t)
    original_f.append(tmp_f.copy())
    for j in range(FILTER_POINT, NUM_POINT - FILTER_POINT):
        tmp_f[j] = 0
    filtered_f.append(tmp_f.copy())
    filtered_t.append(np.real(ifft(tmp_f)))

    sf.write(f"./inter/{i}_original.wav", np.real(tmp_t), SAMPLE_RATE, format="WAV")
    sf.write(f"./inter/{i}_filtered.wav", np.real(filtered_t[i]), SAMPLE_RATE, format="WAV")
    plot_time(original_t[i], f"WAVE {i} TIME, not filtered", f"time_{i}_n")
    plot_time(filtered_t[i], f"WAVE {i} TIME, filtered", f"time_{i}")
    plot_freq(np.real(original_f[i]), f"WAVE {i} FREQ, not filtered", f"freq_{i}_n")
    plot_freq(np.real(filtered_f[i]), f"WAVE {i} FREQ, filtered", f"freq_{i}")

zero = np.zeros(60000)
mixed_f = np.hstack((
    filtered_f[0][0:120000], zero, filtered_f[1][0:120000], zero,
    filtered_f[2][0:120000], zero, filtered_f[3][0:120000], zero,
    zero, filtered_f[3][120000:240000], zero, filtered_f[2][120000:240000],
    zero, filtered_f[1][120000:240000], zero, filtered_f[0][120000:240000]
))
mixed_t = np.real(ifft(mixed_f))
sf.write(f"./output/mixed.wav", np.real(mixed_t), 48000, format="WAV")
plt.figure()
plt.plot(np.linspace(0, 2, LENGTH * 48000), np.real(mixed_f))
plt.title("ENCODED FREQ")
plt.savefig("./fig/freq_encoded.png")
# plt.show()
plt.close()
plt.figure()
plt.plot(np.linspace(0, 30, LENGTH * 48000), np.real(mixed_t))
plt.title("ENCODED TIME")
plt.savefig("./fig/time_encoded.png")
# plt.show()
plt.close()

decoded_f = list()
decoded_t = list()

mixed_f = fft(mixed_t)
for i in range(4):
    lb = i * 6000 * LENGTH
    rb = (8 - i) * 6000 * LENGTH
    tmp_f = np.hstack((mixed_f[lb:lb + 120000], mixed_f[rb - 120000:rb]))
    decoded_f.append(tmp_f.copy())
    tmp_t = np.real(ifft(tmp_f))
    decoded_t.append(tmp_t.copy())
    sf.write(f"./output/decoded_{i}.wav", np.real(tmp_t), SAMPLE_RATE, format="WAV")
    plot_time(decoded_t[i], f"DECODED {i} TIME", f"time_{i}_decoded")
    print(f"WAVE {i} MSE")
    print(f"\tDecoded vs. Original: {mean_squared_error(tmp_t, original_t[i])}")
    print(f"\tDecoded vs. Filtered: {mean_squared_error(tmp_t, filtered_t[i])}")
