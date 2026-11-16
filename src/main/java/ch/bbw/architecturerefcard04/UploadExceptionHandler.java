package ch.bbw.architecturerefcard04;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Zeigt statt der Whitelabel-Error-Page eine Meldung, wenn die Datei
 * grösser als spring.servlet.multipart.max-file-size ist.
 */
@ControllerAdvice
public class UploadExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleTooLarge(RedirectAttributes attributes) {
        attributes.addFlashAttribute("message",
                "Die Datei ist zu gross (maximal 5 MB).");
        return "redirect:/";
    }
}
