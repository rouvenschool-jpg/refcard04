package ch.bbw.architecturerefcard04;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkException;

@Controller
public class MainController {

	// https://spring.io/guides/gs/uploading-files/

	private static final Logger log = LoggerFactory.getLogger(MainController.class);

	private S3StorageService storageService;

	@Autowired
	public MainController(S3StorageService storageService) {
		this.storageService = storageService;
	}

	@GetMapping("")
	public String homepage(@RequestParam(value = "path", defaultValue = "") String path,
						   Model model) {
		String folder = normalizeFolder(path);
		StorageListing listing;
		try {
			listing = storageService.list(folder);
		} catch (SdkException e) {
			log.error("Dateien konnten nicht aus S3 geladen werden", e);
			listing = StorageListing.empty();
			model.addAttribute("message", "Dateien konnten nicht geladen werden: " + describe(e));
		}
		model.addAttribute("folders", listing.getFolders());
		model.addAttribute("files", listing.getFiles());
		model.addAttribute("currentPath", folder);
		model.addAttribute("parentPath", parentOf(folder));
		return "index"; // index.html
	}

	/** Macht aus dem Parameter einen gültigen Ordnerpfad: "" oder "a/b/" (ohne führendes "/"). */
	static String normalizeFolder(String path) {
		String p = path.trim();
		while (p.startsWith("/")) {
			p = p.substring(1);
		}
		if (!p.isEmpty() && !p.endsWith("/")) {
			p = p + "/";
		}
		return p;
	}

	/** Pfad des übergeordneten Ordners ("" = oberste Ebene), null wenn schon ganz oben. */
	static String parentOf(String folder) {
		if (folder.isEmpty()) {
			return null;
		}
		String withoutSlash = folder.substring(0, folder.length() - 1);
		return withoutSlash.substring(0, withoutSlash.lastIndexOf('/') + 1);
	}


	@PostMapping("/upload")
	public String uploadFile(@RequestParam("file") MultipartFile file,
							 RedirectAttributes attributes) {

		if (file.isEmpty()) {
			attributes.addFlashAttribute("message", "Bitte eine Datei auswählen.");
			return "redirect:/";
		}

		try {
			storageService.storeFile(file);
			attributes.addFlashAttribute("message",
					"Datei \"" + file.getOriginalFilename() + "\" hochgeladen.");
		} catch (RuntimeException e) {
			log.error("Upload nach S3 fehlgeschlagen", e);
			attributes.addFlashAttribute("message", "Upload fehlgeschlagen: " + describe(e));
		}
		return "redirect:/";
	}

	/** Kurze, verständliche Fehlerbeschreibung für die Anzeige im Browser. */
	static String describe(RuntimeException e) {
		if (e instanceof AwsServiceException) {
			AwsServiceException aws = (AwsServiceException) e;
			String code = aws.awsErrorDetails() != null ? aws.awsErrorDetails().errorCode() : null;
			return "S3 meldet " + (code != null ? code : "Status " + aws.statusCode())
					+ " (Zugangsdaten abgelaufen, Bucket oder Berechtigung prüfen).";
		}
		if (e instanceof SdkException) {
			return "S3 nicht erreichbar oder keine Zugangsdaten gefunden.";
		}
		return "unerwarteter Fehler (siehe Server-Log).";
	}

}
