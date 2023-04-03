package dev.gyuray.GPTeacherDemo;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

@Controller
public class MainController {

    static {
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.FINE);
        Logger log = LogManager.getLogManager().getLogger("");
        log.addHandler(handler);
        log.setLevel(Level.FINE);
        System.setProperty("javax.net.debug","all");
    }

    private final String API_KEY = "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";

    @GetMapping("/")
    public String goHome() {

        return "home";
    }

    @PostMapping("/gpt/correction")
    public String correct(
            @RequestParam String userScript,
            Model model
    ) throws IOException {

        List<JSONObject> messages = new ArrayList<>();

        // 과거 이력 프롬프트에 추가 - 유저 스크립트
        JSONObject priorPrompt = new JSONObject();
        priorPrompt.put("role", "user");
        priorPrompt.put("content", ChatGPT.SPEAKING_INSTRUCTION_PREFIX + ChatGPT.EXAMPLE_SCRIPT);
        messages.add(priorPrompt);

        // 과거 이력 프롬프트에 추가 - 교정 스크립트
        JSONObject priorResponse = new JSONObject();
        priorResponse.put("role", "assistant");
        priorResponse.put("content", ChatGPT.EXAMPLE_SENTENCE_JSON_ARRAY.toString());
        messages.add(priorResponse);

        // 교정 지시문 + 유저 스크립트 프롬프트 설정
        JSONObject message = new JSONObject();
        message.put("role", "user");
        String prompt = ChatGPT.SPEAKING_INSTRUCTION_PREFIX + userScript;
        message.put("content", prompt);
        messages.add(message);

        // ChatGPT에 메시지 요청
        ChatGPT chatGPT = new ChatGPT();
        String responseText = chatGPT.chat(messages);

        model.addAttribute("userScript", userScript);
        model.addAttribute("correctedScript", responseText);

        return "home";
    }

    @PostMapping("/gpt/split")
    public String split(
            @RequestParam String userScript,
            @RequestParam String correctedScript,
            Model model
    ) throws IOException {

        List<JSONObject> messages = new ArrayList<>();

        // 과거 이력 프롬프트에 추가 - 유저 스크립트
        JSONObject priorPrompt = new JSONObject();
        priorPrompt.put("role", "user");
        String prompt = ChatGPT.SPEAKING_INSTRUCTION_PREFIX + userScript;
        System.out.println("prompt = " + ChatGPT.SPEAKING_INSTRUCTION_PREFIX);
        priorPrompt.put("content", prompt);
        messages.add(priorPrompt);

        // 과거 이력 프롬프트에 추가 - 교정 스크립트
        JSONObject priorResponse = new JSONObject();
        priorResponse.put("role", "assistant");
        priorResponse.put("content", ChatGPT.EXAMPLE_SENTENCE_JSON_ARRAY.toString());
        messages.add(priorResponse);

        // 스크립트 분할 지시 프롬프트에 추가
        JSONObject splitPrompt = new JSONObject();
        splitPrompt.put("role", "user");
        splitPrompt.put("content", ChatGPT.SPLIT_INSTRUCTION);
        messages.add(splitPrompt);

        // 분할 요청
        ChatGPT chatGPT = new ChatGPT();
        String responseText = chatGPT.chat(messages).trim();

        // 문장 개수에 따른 처리
        JSONArray splitScript = new JSONArray();
        try {
            if (responseText.startsWith("[")) { // 문장이 2개 이상 -> JSON 배열로 반환됨
                splitScript = new JSONArray(responseText);
            } else if (responseText.startsWith("{")) { // 문장이 1개 -> JSON 객체로 반환됨
                splitScript.put(new JSONObject(responseText));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        model.addAttribute("userScript", userScript);
        model.addAttribute("correctedScript", correctedScript);
        model.addAttribute("splitScript", splitScript);

        return "home";
    }

    @GetMapping("/whisper")
    public String getSttForm() {
        return "sttRecord";
    }

    @ResponseBody
    @PostMapping("/whisper")
    public String transcript(
            @RequestParam MultipartFile audio
    ) throws IOException {

        String fileName = UUID.randomUUID().toString() + ".webm";
        String filePath = "/Users/gyuray/dev/projects/GPTeacherDemo/src/main/resources/static/files/" + fileName;
        File uploadFile = new File(filePath);
        audio.transferTo(uploadFile);
        StringBuffer response = new StringBuffer(); // 응답 받은 문자

        try {
            String apiURL = "https://api.openai.com/v1/audio/transcriptions";
            URL url = new URL(apiURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setUseCaches(false);
            con.setDoOutput(true);
            con.setDoInput(true);

            String LINE_FEED = "\r\n";
            String boundary = "----" + UUID.randomUUID();
            con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            con.setRequestProperty("Authorization", "Bearer " + API_KEY);
            OutputStream outputStream = con.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true);

            writer.append("--" + boundary).append(LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"model\"").append(LINE_FEED);
            writer.append(LINE_FEED);
            writer.append("whisper-1").append(LINE_FEED);
            writer.append("--" + boundary).append(LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + uploadFile.getName() + "\"").append(LINE_FEED);
            writer.append("Content-Type: audio/webm").append(LINE_FEED);
            writer.append(LINE_FEED);
            writer.flush();

            FileInputStream inputStream = new FileInputStream(uploadFile);
            byte[] buffer = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            inputStream.close();

            writer.flush();
            writer.append("--" + boundary + "--").append(LINE_FEED);
            writer.close();

            BufferedReader br = null;
            int responseCode = con.getResponseCode();
            if (responseCode == 200) { // 정상 호출
                br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            } else {  // 오류 발생
                System.out.println("error!!!!!!! responseCode= " + responseCode);
                br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            }
            String inputLine;

            if (br != null) {
                while ((inputLine = br.readLine()) != null) {
                    response.append(inputLine);
                }
                br.close();
                System.out.println("response = " + response.toString());
            } else {
                System.out.println("error !!!");
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        try {
            JSONObject jsonObject = new JSONObject(response.toString());
            String text = jsonObject.getString("text");
            System.out.println("text = " + text);
            return text;
        } catch (Exception e) {
            String text = new JSONObject(response.toString()).getJSONObject("error").getString("message");
            System.out.println("text = " + text);
            return text;
        }
    }

}
