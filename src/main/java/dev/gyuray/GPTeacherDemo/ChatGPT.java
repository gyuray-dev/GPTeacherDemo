package dev.gyuray.GPTeacherDemo;

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

    // ChatGPT에게 문장을 교정해달라고 요청하는 프롬프트
    public static final String SPEAKING_INSTRUCTION =
            "Provide me a feedback on my speaking script in terms of grammar and clarity." +
            "This is an answer for a question that is given to me to answer in an English speaking test like OPIc, TOEIC Speaking, etc." +
            "Don't append any unnecessary comment in your answer except the corrected speaking script." +
            "Here it is : \n";

    // ChatGPT에게 JSON 형식으로 보내달라고 요청할 때 예시 문자열로 전환하기 위한 JSON 객체
    private static final JSONObject EXAMPLE_SENTENCE_JSON = new JSONObject()
            .put("original", "I think there are so many questions about traveling at OPIC test.")
            .put("corrected", "I think there are so many questions about traveling at OPIC test.")
            .put("explanation", "The word \"traveling\" should be changed to \"travel\" and \"OPIC test\" should be \"OPIC test.\"");

    // ChatGPT에게 스크립트를 문장별로 나눠달라고 요청하는 프롬프트
    public static final String SPLIT_INSTRUCTION =
            "Split the corrected version of script into individual sentences with their original version" +
            "with explanation for each of them." +
            "Give me the response as a string that can be parsed into JSON without any additional comments " +
            "because I want to directly parse the string you reply to me to a JSON object" +
            "Here is an example for the format of answer I want to receive: \n" + EXAMPLE_SENTENCE_JSON.toString();

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
