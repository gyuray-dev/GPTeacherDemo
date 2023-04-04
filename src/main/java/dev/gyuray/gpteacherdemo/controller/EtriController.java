package dev.gyuray.gpteacherdemo.controller;

import dev.gyuray.gpteacherdemo.api.Etri;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;

@Controller
public class EtriController {

    @Autowired
    private Etri etri;

    @GetMapping("/etri/pronunciation")
    public String getPronunciationForm() {
        return "pronunciation";
    }

    @ResponseBody
    @PostMapping("/etri/pronunciation")
    public String evaluatePronunciation(
            @RequestParam MultipartFile audio
    ) throws IOException, UnsupportedAudioFileException {

        String result = etri.pronunciation(audio);

        return result;
    }
}
