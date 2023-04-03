package dev.gyuray.GPTeacherDemo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class ChatGPT {

    private final String URL = "https://api.openai.com/v1/chat/completions";
    private final String API_KEY = "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";

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

    // ChatGPT에게 스크립트를 문장별로 나눠달라고 요청하는 프롬프트
    public static final String SPLIT_INSTRUCTION =
            "1. Give me the response as a string type that can be parsed into JSON with multiple JSON object. \n" +
            "2. One JSON object represents just only one sentence. " +
            "3. There must be as many JSON objects as there are sentences. " +
            "4. Do not append any unnecessary comments. " +
            "Here is an example of response format if there is two sentence: \n\n" + EXAMPLE_SENTENCE_JSON_ARRAY.toString();

    public String chat(List<JSONObject> messages) throws IOException {

        // HTTP 통신 설정
        HttpURLConnection con = (HttpURLConnection) new URL(URL).openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Authorization", "Bearer " + API_KEY);

        // 요청 옵션 설정
        JSONObject prompt = new JSONObject();
        prompt.put("model", "gpt-3.5-turbo-0301"); // required - ChatGPT 3.5 모델 - 현재 우리가 무료로 쓰는 모델
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
}
