package guru.springframework.springaiimage.services;

import guru.springframework.springaiimage.model.Question;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.util.Base64;

/**
 * Created by jt, Spring Framework Guru.
 */
@Service
public class OpenAIServiceImpl implements OpenAIService {

    private final ImageModel imageModel;
    private final String imageModelName;

    public OpenAIServiceImpl(ImageModel imageModel,
                             @Value("${app.openai.image-model:gpt-image-1}") String imageModelName) {
        this.imageModel = imageModel;
        this.imageModelName = imageModelName;
    }

    @Override
    public byte[] getImage(Question question) {

        var options = OpenAiImageOptions.builder()
                .withHeight(1024).withWidth(1024)
                .withModel(imageModelName)
                .build();

        ImagePrompt imagePrompt = new ImagePrompt(question.question(), options);

        var imageResponse = imageModel.call(imagePrompt);

        var output = imageResponse.getResult().getOutput();

        if (output.getB64Json() != null && !output.getB64Json().isBlank()) {
            return Base64.getDecoder().decode(output.getB64Json());
        }

        if (output.getUrl() != null && !output.getUrl().isBlank()) {
            try (var inputStream = URI.create(output.getUrl()).toURL().openStream()) {
                return inputStream.readAllBytes();
            } catch (IOException e) {
                throw new IllegalStateException("Unable to download generated image from URL.", e);
            }
        }

        throw new IllegalStateException("Image model returned neither b64Json nor URL output.");
    }
}















