package dev.gyuray.gpteacherdemo.api;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Component
public class OpenAi {

    private final String URL_CHAT = "https://api.openai.com/v1/chat/completions";
    private final String URL_TRANSCRIPT = "https://api.openai.com/v1/audio/transcriptions";

    @Value("${openai.api.key}")
    private String API_KEY;

    @Value("${file.dir}")
    private String fileDir;

    // ChatGPT에게 보여줄 응답 예제
    public static final String EXAMPLE_SCRIPT =
            "I think there's so many questions about traveling at OPIC test " +
                    "but I don't like travels that much especially the travel aboard " +
                    "so I barely remember the trouble that I had ";

    // ChatGPT에게 보여줄 JSON 예제
    public static final JSONObject EXAMPLE_SENTENCE1_JSON = new JSONObject()
            .put("original", "I think there's so many questions about traveling at OPIC test.")
            .put("corrected", "I think there are many questions about traveling on the OPIC test.")
            .put("explanation", "\"There's\" is a contraction of \"there is,\" which is singular. However, \"questions\" is plural, so \"there are\" should be used instead.");

    public static final JSONObject EXAMPLE_SENTENCE2_JSON = new JSONObject()
            .put("original", "but I don't like travels that much especially the travel aboard so I barely remember the trouble that I had.")
            .put("corrected", "However, I don't enjoy traveling that much, especially abroad, so I barely remember any trouble I may have had.")
            .put("explanation", "\"Travels\" should be changed to \"traveling\" to make it a verb. \"Aboard\" should be changed to \"abroad\" to indicate traveling to another country. The sentence structure is also improved for clarity.\"");

    public static JSONArray EXAMPLE_SENTENCE_JSON_ARRAY = new JSONArray()
            .put(EXAMPLE_SENTENCE1_JSON)
            .put(EXAMPLE_SENTENCE2_JSON);

    // ChatGPT에게 문장을 교정해달라고 요청하는 프롬프트
    public static final String SPEAKING_INSTRUCTION_PREFIX =
            "Cut my speaking script by sentence one by one followed by corrected one and explanation in JSON array. " +
                    "JSON object has three attribute. \n" +
                    "1. original: original sentence. \n" +
                    "2. corrected: corrected sentence. \n" +
                    "3. explanation: explanation for correction. \n" +
                    "Here is the script : \n";

    public String chat(List<JSONObject> messages) throws IOException {

        // HTTP 통신 설정
        HttpURLConnection con = (HttpURLConnection) new URL(URL_CHAT).openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Authorization", "Bearer " + API_KEY);

        // 요청 옵션 설정
        JSONObject prompt = new JSONObject();
        prompt.put("model", "gpt-3.5-turbo-0301"); // required - ChatGPT 3.5 모델 - 현재 공개된 모델
        prompt.put("messages", messages); // required - 요청 프롬프트(과거 대화 이력 포함)
        prompt.put("temperature", 0.0); // optional - 0 ~ 2 (클수록 답변이 랜덤해짐)

        // HTTP 요청
        con.setDoOutput(true);
        con.getOutputStream().write(prompt.toString().getBytes());

        // 결과 수신
        String output = new BufferedReader(new InputStreamReader(con.getInputStream()))
                .lines()
                .reduce((a, b) -> a + b)
                .get();

        System.out.println("output = " + output);

        // 수신한 JSON에서 응답 메시지 파싱
        String responseText = new JSONObject(output)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");

        return responseText;
    }

    public String transcript(MultipartFile audio) throws IOException {
        String fileName = UUID.randomUUID() + ".webm";
        String filePath = fileDir + fileName;
        File audioFile = new File(filePath);
        audio.transferTo(audioFile);

        // HTTP 통신 설정
        URL url = new URL(URL_TRANSCRIPT);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setUseCaches(false);
        con.setDoOutput(true);
        con.setDoInput(true);

        String LINE_FEED = "\r\n";
        String boundary = "----" + UUID.randomUUID();
        con.setRequestProperty("Authorization", "Bearer " + API_KEY);
        con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        OutputStream outputStream = con.getOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true);

        // request body
        writer.append("--" + boundary).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + audioFile.getName() + "\"").append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.flush();

        FileInputStream inputStream = new FileInputStream(audioFile);
        byte[] buffer = new byte[4096];
        int bytesRead = -1;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();
        inputStream.close();

        writer.append(LINE_FEED);
        writer.append("--" + boundary).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"model\"").append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.append("whisper-1").append(LINE_FEED);
        writer.append("--" + boundary + "--").append(LINE_FEED);
        writer.close();

        // 결과 수신
        String output = new BufferedReader(new InputStreamReader(con.getInputStream()))
                .lines()
                .reduce((a, b) -> a + b)
                .get();

        System.out.println("output = " + output);

        JSONObject jsonObject = new JSONObject(output);
        String script = jsonObject.getString("text");

        return script;
    }
}
