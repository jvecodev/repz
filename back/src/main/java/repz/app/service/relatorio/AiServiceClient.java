package repz.app.service.relatorio;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class AiServiceClient {

    private final RestClient restClient;

    public AiServiceClient(@Value("${ai.service.url:http://localhost:8055}") String aiServiceUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(120_000);
        this.restClient = RestClient.builder()
                .baseUrl(aiServiceUrl)
                .requestFactory(factory)
                .build();
    }

    public String gerarRelatorio(String prompt) {
        log.info("Chamando AI service...");
        long start = System.currentTimeMillis();
        ReportRequest request = new ReportRequest(prompt);
        ReportResponse response = restClient.post()
                .uri("/report")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ReportResponse.class);
        log.info("AI service respondeu em {}ms", System.currentTimeMillis() - start);
        return response != null ? response.content() : "";
    }

    record ReportRequest(String prompt,
                         String system_prompt) {
        ReportRequest(String prompt) {
            this(prompt,
                 "Você é um especialista em avaliação física e saúde. " +
                 "Gere relatórios claros, motivadores e baseados em evidências sobre a evolução do aluno.");
        }
    }

    record ReportResponse(String content) {}
}
