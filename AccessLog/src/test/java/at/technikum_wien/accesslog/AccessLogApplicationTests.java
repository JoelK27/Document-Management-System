package at.technikum_wien.accesslog;

import at.technikum_wien.accesslog.repo.AccessStatRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
// Wir nutzen EnableAutoConfiguration, um spezifische Configs hart auszuschließen
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
class AccessLogApplicationTests {

    @MockitoBean
    private AccessStatRepository accessStatRepository;

    @Test
    void contextLoads() {
    }

}