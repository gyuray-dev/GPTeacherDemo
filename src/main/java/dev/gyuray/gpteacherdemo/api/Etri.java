package dev.gyuray.gpteacherdemo.api;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

@Component
public class Etri {

    @Value("${etri.api.key}")
    private String API_KEY;
    @Value("${file.dir}")
    private String fileDir;
    private final String URL = "http://aiopen.etri.re.kr:8000/WiseASR/Pronunciation";

    public String pronunciation(MultipartFile audio) throws IOException, UnsupportedAudioFileException {

        String uuid = UUID.randomUUID().toString();
        String fileName = uuid + ".wav";
        String filePath = fileDir + fileName;
        File audioFile = new File(filePath);
        audio.transferTo(audioFile);

        File rawPcm = getRawPcm(audioFile);

        URL url = new URL(URL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoOutput(true);

        con.setRequestProperty("Authorization", API_KEY);
        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        con.setRequestMethod("POST");

        byte[] audioBytes = Files.readAllBytes(rawPcm.toPath());
        String audioContents = Base64.getEncoder().encodeToString(audioBytes);

        JSONObject argument = new JSONObject();
        argument.put("language_code", "english");
        argument.put("script", "Hi, nice to meet you."); // 유저가 읽어야 할 스크립트
        argument.put("audio", audioContents);

        JSONObject request = new JSONObject();
        request.put("argument", argument);

        OutputStream os = con.getOutputStream();
        DataOutputStream dos = new DataOutputStream(os);
        dos.write(request.toString().getBytes(StandardCharsets.UTF_8));
        dos.flush();
        dos.close();

        System.out.println("responseCode = " + con.getResponseCode());
        String output = new BufferedReader(new InputStreamReader(con.getInputStream()))
                .lines()
                .reduce((a, b) -> a + b)
                .get();

        return output;
    }

    public File getRawPcm(File wav) throws IOException, UnsupportedAudioFileException {

        final AudioFormat AUDIO_FORMAT = new AudioFormat(
                16_000,
                16,
                1,
                true,
                false
        );

        // Audio To Byte
        FileInputStream fis = new FileInputStream(wav);
        BufferedInputStream bis = new BufferedInputStream(fis);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buff = new byte[4096];
        int read = -1;
        while ((read = bis.read(buff)) != -1 ) {
            baos.write(buff, 0, read);
        }
        baos.flush();
        byte[] audioBytes = baos.toByteArray();
        System.out.println("audioBytes.length = " + audioBytes.length);

        // format .wav -> .wav
        AudioInputStream originalAis = AudioSystem.getAudioInputStream(new ByteArrayInputStream(audioBytes));
        AudioInputStream formattedAis = AudioSystem.getAudioInputStream(AUDIO_FORMAT, originalAis);
        AudioInputStream lengthAddedAis = new AudioInputStream(formattedAis, AUDIO_FORMAT, audioBytes.length);

        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        AudioSystem.write(lengthAddedAis, AudioFileFormat.Type.WAVE, baos2);
        byte[] newAudioBytes = baos.toByteArray();

        // format .wav to .raw (헤더 제거) 경우에 따라 46바이트일 수도 있으니 오류 발생 시 테스트
        // https://stackoverflow.com/questions/19991405/how-can-i-detect-whether-a-wav-file-has-a-44-or-46-byte-header
        byte[] rawBytes = Arrays.copyOfRange(newAudioBytes, 44, newAudioBytes.length);

        int dot = wav.getName().lastIndexOf(".");
        String pcmFilePath = fileDir + wav.getName().substring(0, dot) + ".pcm";
        FileOutputStream fos = new FileOutputStream(pcmFilePath);
        fos.write(rawBytes);

        File pcm = new File(pcmFilePath);

        return pcm;
    }

}
