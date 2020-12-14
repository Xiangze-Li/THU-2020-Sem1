from pathlib import Path
import librosa
import librosa.display
import matplotlib.pyplot as plt
from scipy import signal
import numpy as np
import soundfile

AUDIO_NUM = 4
FILTER_FREQ_MAX = 3400
BUTTER_ORDER = 10
SAMPLE_RATE = 8000

Path("log/wave").mkdir(parents=True, exist_ok=True)
Path("log/freq").mkdir(parents=True, exist_ok=True)
Path("log/audio").mkdir(parents=True, exist_ok=True)


def plot_wave(wave, name):
    plt.figure()
    librosa.display.waveplot(wave, sr=SAMPLE_RATE)
    plt.savefig(f'log/wave/{name}.png')
    plt.close()


def plot_freq(wave, name):
    freq = np.abs(np.fft.fft(wave))
    plt.figure()
    plt.plot(freq)
    plt.savefig(f'log/freq/{name}.png')
    plt.close()


def plot_all(wave, name):
    plot_wave(wave, name)
    plot_freq(wave, name)


def save_audios(waves, name):
    for i in range(AUDIO_NUM):
        soundfile.write(f'log/audio/{i}_{name}.wav', waves[i], SAMPLE_RATE)


def preprocess():
    audios = []
    for i in range(AUDIO_NUM):
        y, read_sample_rate = librosa.load(f'data/{i}.mp3', duration=30)
        plot_all(y, f'{i}_original')
        numer, denomi = signal.butter(BUTTER_ORDER, FILTER_FREQ_MAX, fs=read_sample_rate)
        y_filter = signal.filtfilt(numer, denomi, y)
        y_resample = librosa.resample(y_filter, read_sample_rate, SAMPLE_RATE)
        plot_all(y_resample, f'{i}_preprocessed')
        audios.append(y_resample)
    return audios


def get_filter_para():
    numers, denomis = [], []
    freqs = [0.25, [0.25, 0.5], [0.5, 0.75], 0.75]
    filter_types = ['lowpass', 'bandpass', 'bandpass', 'highpass']
    for freq, btype in zip(freqs, filter_types):
        numer, denomi = signal.butter(BUTTER_ORDER, freq, btype=btype)
        numers.append(numer)
        denomis.append(denomi)
    return numers, denomis


def encode(audios):
    def upsample(wave):
        up_wave = np.zeros(AUDIO_NUM * len(wave))
        up_wave[::AUDIO_NUM] = wave
        return up_wave

    def filter_all(waves):
        filter_waves = []
        numers, denomis = get_filter_para()
        # freqs = [0.25, [0.25, 0.5], [0.5, 0.75], 0.75]
        # filter_types = ['lowpass', 'bandpass', 'bandpass', 'highpass']
        for i in range(AUDIO_NUM):
            # numer, denomi = signal.butter(BUTTER_ORDER, freq, btype=btype)
            filter_wave = signal.filtfilt(numers[i], denomis[i], waves[i])
            plot_all(filter_wave, f'{i}_filtered')
            filter_waves.append(filter_wave)
        return filter_waves

    upsampled_audios = []
    for i in range(AUDIO_NUM):
        up_audio = upsample(audios[i])
        plot_all(up_audio, f'{i}_upsample')
        upsampled_audios.append(up_audio)
    filtered_audios = filter_all(upsampled_audios)
    encoded_wave = np.sum(filtered_audios, axis=0)
    plot_all(encoded_wave, 'encoded')
    return encoded_wave


def decode(encoded_wave):
    def downsampe(wave):
        return wave[::AUDIO_NUM] * AUDIO_NUM

    numers, denomis = get_filter_para()
    decoded_waves = []
    for i in range(AUDIO_NUM):
        decoded = signal.filtfilt(numers[i], denomis[i], encoded_wave)
        decoded = downsampe(decoded)
        plot_all(decoded, f'{i}_decoded')
        decoded_waves.append(decoded)
    return decoded_waves


def main():
    audios = preprocess()
    save_audios(audios, 'preprocessed')
    encoded = encode(audios)
    decoded = decode(encoded)
    save_audios(decoded, 'decoded')
    mse = np.mean(np.sum((np.array(audios) - np.array(decoded)) ** 2, axis=1))
    print(mse)


if __name__ == '__main__':
    main()
