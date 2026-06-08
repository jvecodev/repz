package repz.app.controller;

import org.junit.jupiter.api.Test;
import repz.app.controller.impl.HealthImplController;

import static org.assertj.core.api.Assertions.assertThat;

class HealthControllerUnitTest {

    private final HealthImplController controller = new HealthImplController();

    @Test
    void healthRetornaStatusUp() {
        var resp = controller.health();
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).containsEntry("status", "UP");
    }
}
