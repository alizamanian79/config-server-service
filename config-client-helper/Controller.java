


import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RefreshScope // add refresh scope
@RestController
@RequestMapping
public class Controller {

    @Value("${application.value}") // value from cloud (Github repo)
    private String value;

    @GetMapping
    public String ss() {
        return "hello " + value;
    }

   
}
