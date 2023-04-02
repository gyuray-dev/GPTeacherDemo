package dev.gyuray.GPTeacherDemo;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
public class MainController {

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

        // 교정 지시문 + 유저 스크립트 프롬프트 설정
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", ChatGPT.SPEAKING_INSTRUCTION + userScript);
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
            @RequestParam String rawScript,
            @RequestParam String correctedScript,
            Model model
    ) throws IOException {

        List<JSONObject> messages = new ArrayList<>();

        // 과거 이력 프롬프트에 추가 - 유저 스크립트
        JSONObject priorPrompt = new JSONObject();
        priorPrompt.put("role", "user");
        priorPrompt.put("content", ChatGPT.SPEAKING_INSTRUCTION + rawScript);
        messages.add(priorPrompt);

        // 과거 이력 프롬프트에 추가 - 교정 스크립트
        JSONObject priorResponse = new JSONObject();
        priorResponse.put("role", "assistant");
        priorResponse.put("content", correctedScript);
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

        model.addAttribute("rawScript", rawScript);
        model.addAttribute("correctedScript", correctedScript);
        model.addAttribute("splitScript", splitScript);

        return "home";
    }
}
