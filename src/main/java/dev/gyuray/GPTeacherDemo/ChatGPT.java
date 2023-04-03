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
    // ChatGPT에게 보여줄 JSON 예제
    private static final JSONObject EXAMPLE_SENTENCE1_JSON = new JSONObject()
            .put("original", "original sentence1")
            .put("corrected", "corrected sentence1")
            .put("explanation", "explanation1");

    private static final JSONObject EXAMPLE_SENTENCE2_JSON = new JSONObject()
            .put("original", "original sentence2")
            .put("corrected", "corrected sentence2")
            .put("explanation", "explanation2");

    private static JSONArray EXAMPLE_SENTENCE_JSON_ARRAY = new JSONArray()
            .put(EXAMPLE_SENTENCE1_JSON)
            .put(EXAMPLE_SENTENCE2_JSON);

    // ChatGPT에게 문장을 교정해달라고 요청하는 프롬프트
    public static final String SPEAKING_INSTRUCTION_PREFIX =
            "You are my English tutor. " +
            "Please split my speaking script into individual sentences followed by corrected sentence and explanation. " +
            "Here is the script : \n";

    public static final String SPEAKING_INSTRUCTION_SUFFIX =
            "Here is an example: \n" + EXAMPLE_SENTENCE_JSON_ARRAY.toString();

    // ChatGPT에게 스크립트를 문장별로 나눠달라고 요청하는 프롬프트
    public static final String SPLIT_INSTRUCTION =
            "1. Give me the response as a string type that can be parsed into JSON array with multiple JSON object. \n" +
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
