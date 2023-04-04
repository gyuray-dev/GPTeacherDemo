package dev.gyuray.gpteacherdemo.controller;

import dev.gyuray.gpteacherdemo.api.OpenAi;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Controller
public class OpenAiController {

    @Autowired
    private OpenAi openAi;

    @GetMapping("/gpt/correction")
    public String getCorrect() {
        return "correction";
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
        priorPrompt.put("content", OpenAi.SPEAKING_INSTRUCTION_PREFIX + OpenAi.EXAMPLE_SCRIPT);
        messages.add(priorPrompt);

        // 과거 이력 프롬프트에 추가 - 교정 스크립트
        JSONObject priorResponse = new JSONObject();
        priorResponse.put("role", "assistant");
        priorResponse.put("content", OpenAi.EXAMPLE_SENTENCE_JSON_ARRAY.toString());
        messages.add(priorResponse);

        // 교정 지시문 + 유저 스크립트 프롬프트 설정
        JSONObject message = new JSONObject();
        message.put("role", "user");
        String prompt = OpenAi.SPEAKING_INSTRUCTION_PREFIX + userScript;
        message.put("content", prompt);
        messages.add(message);

        // 분할 요청
        String responseText = openAi.chat(messages).trim();

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
        model.addAttribute("splitScript", splitScript);

        return "correction";
    }

    @PostMapping("/gpt/split")
    public String split(
            @RequestParam String userScript,
            @RequestParam String correctedScript,
            Model model
    ) throws IOException {

        List<JSONObject> messages = new ArrayList<>();

        // 가상 과거 이력 프롬프트에 추가 - 유저 스크립트
        JSONObject priorPrompt = new JSONObject();
        priorPrompt.put("role", "user");
        String prompt = OpenAi.SPEAKING_INSTRUCTION_PREFIX + userScript;
        priorPrompt.put("content", prompt);
        messages.add(priorPrompt);

        // 가상 과거 이력 프롬프트에 추가 - 교정 스크립트
        JSONObject priorResponse = new JSONObject();
        priorResponse.put("role", "assistant");
        priorResponse.put("content", OpenAi.EXAMPLE_SENTENCE_JSON_ARRAY.toString());
        messages.add(priorResponse);

        // 스크립트 분할 지시 프롬프트에 추가
        JSONObject splitPrompt = new JSONObject();
        splitPrompt.put("role", "user");
        splitPrompt.put("content", OpenAi.SPLIT_INSTRUCTION);
        messages.add(splitPrompt);

        // 분할 요청
        String responseText = openAi.chat(messages).trim();

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

        return "correction";
    }

    @GetMapping("/gpt/whisper")
    public String getSttForm() {
        return "whisper";
    }

    @ResponseBody
    @PostMapping("/gpt/whisper")
    public String transcript(
            @RequestParam MultipartFile audio
    ) throws IOException {

        String script = openAi.transcript(audio);

        return script;
    }

}
