package guru.springframework.springaiimage.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import guru.springframework.springaiimage.model.Question;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import java.util.List;

/**
 * Created by jt, Spring Framework Guru.
 */
@Service
public class OpenAIServiceImpl implements OpenAIService {

    private final RestClient restClient;
    private final String apiKey;
    private final String imageModelName;
    private final String imagePath;

    public OpenAIServiceImpl(@Value("${spring.ai.openai.base-url:https://api.openai.com}") String baseUrl,
                             @Value("${OPENAI_API_KEY:}") String apiKey,
                             @Value("${app.openai.image-model:gpt-image-1}") String imageModelName,
                             @Value("${app.openai.image-path:/v1/images/generations}") String imagePath) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
        this.apiKey = apiKey;
        this.imageModelName = imageModelName;
        this.imagePath = imagePath;
    }

    @Override
    public byte[] getImage(Question question) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY must be set.");
        }

        var request = new ImageGenerationRequest(imageModelName, question.question());
        var response = restClient.post()
                .uri(imagePath)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ImageGenerationResponse.class);

        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new IllegalStateException("Image generation response did not contain image data.");
        }

        var imageData = response.data().getFirst();
        if (imageData.b64Json() != null && !imageData.b64Json().isBlank()) {
            return Base64.getDecoder().decode(imageData.b64Json());
        }
        if (imageData.url() != null && !imageData.url().isBlank()) {
            try (var inputStream = URI.create(imageData.url()).toURL().openStream()) {
                return inputStream.readAllBytes();
            } catch (IOException e) {
                throw new IllegalStateException("Unable to download generated image from URL.", e);
            }
        }

        throw new IllegalStateException("Image output contained neither b64_json nor url.");
    }

    private record ImageGenerationRequest(String model, String prompt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ImageGenerationResponse(List<ImageData> data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ImageData(@JsonProperty("b64_json") String b64Json, String url) {
    }
}














