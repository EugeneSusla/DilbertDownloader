package dilbertdownloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;

/**
 * 
 * @author Eugene Susla
 */
public class DilbertDownloader {

	private static final Logger LOGGER = Logger
			.getLogger(DilbertDownloader.class.getName());

	private static final ResourceBundle CONFIG = ResourceBundle
			.getBundle("dilbertdownloader.config");

	private static final String beginSequence = CONFIG
			.getString("beginSequence");
	private static final String endSequence = CONFIG.getString("endSequence");
	private static final String targetFolder = CONFIG.getString("targetFolder");
	private static final String baseUrl = CONFIG.getString("baseUrl");
	private static final String domainUrl = CONFIG.getString("domainUrl");
	private static final DateFormat dateToFileFormat = new SimpleDateFormat(
			CONFIG.getString("fileNameFormat"));
	private static final DateFormat dateFormat = new SimpleDateFormat(
			CONFIG.getString("urlDateFormat"));

	private static volatile String input = "";

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) {
		InputStreamReader converter = new InputStreamReader(System.in);
		BufferedReader in = new BufferedReader(converter);

		LOGGER.info("Downloading lastest comics:");
		downloadLastest();

		Thread thread = new Thread(new Runnable() {
			public void run() {
				LOGGER.info("Downloading comics:");
				Calendar counter = findFirstPresentComic();
				int comicsDownloaded = 0;
				while (!Thread.interrupted()) {
					downloadComic(counter, false, true);
					counter.add(Calendar.DAY_OF_MONTH, -1);
					comicsDownloaded++;
				}
				LOGGER.info("Finished downloading. Comics downloaded: "
						+ comicsDownloaded);
			}
		});
		thread.start();
		input = "";
		while (input.isEmpty()) {
			try {
				input = in.readLine();
			} catch (IOException ex) {
				LOGGER.error(null, ex);
			}
		}
		thread.interrupt();
	}

	private static String getURL(Calendar date) {
		String html;
		try {
			html = util.net.Http.getResponsePart(
					baseUrl + dateFormat.format(date.getTime()) + "/",
					beginSequence, endSequence);
		} catch (IOException ex) {
			return null;
		}
		if (html.isEmpty()) {
			return null;
		}
		int beginIndex = html.indexOf(beginSequence)
				+ beginSequence.indexOf('/');
		int endIndex = html.indexOf(endSequence, beginIndex)
				+ endSequence.indexOf('\"');
		String result = domainUrl + html.substring(beginIndex, endIndex);
		return result;
	}

	private static boolean isComicForDateDownloaded(Calendar date) {
		String file = targetFolder + dateToFileFormat.format(date.getTime()) + ".gif";
		LOGGER.debug("Looking for file " + file);
		return new File(file).exists();

	}

	private static boolean downloadComic(Calendar date,
			boolean checkForAlreadyDownloaded, boolean verbose) {
		String fileName = targetFolder
				+ dateToFileFormat.format(date.getTime()) + ".gif";
		if (checkForAlreadyDownloaded && (new File(fileName)).exists()) {
			return false;
		}
		String url = getURL(date);
		if (url == null) {
			LOGGER.warn("There seems to be no comic for date "
					+ dateFormat.format(date.getTime()));
			return false;
		}
		LOGGER.debug("Download url: " + url);
		try {
			new File(fileName).getParentFile().mkdirs();
			util.net.Http.writeResponseToFile(url, fileName);
			if (verbose)
				LOGGER.info("Comic downloaded for date: "
						+ dateFormat.format(date.getTime()));
			return true;
		} catch (IOException ex) {
			LOGGER.error(
					"Error downloading comic for date "
							+ dateFormat.format(date.getTime()) + " : ", ex);
			return false;
		}
	}

	/**
	 * downloads up to <code>limit</code> lastest comics starting from today's
	 * day, going backwards
	 * 
	 * @param limit
	 */
	private static void downloadLastest(int limit) {
		Calendar counter = Calendar.getInstance(); // today
		int count = 0;
		while (!isComicForDateDownloaded(counter)
				&& (limit <= 0 || count < limit)) {
			downloadComic(counter, false, true);
			counter.add(Calendar.DAY_OF_MONTH, -1);
			count++;
		}
	}

	private static void downloadLastest() {
		downloadLastest(0);
	}

	private static Calendar findFirstPresentComic() {
		Calendar counter = Calendar.getInstance();
		while (isComicForDateDownloaded(counter)) {
			counter.add(Calendar.DAY_OF_MONTH, -1);
		}
		counter.add(Calendar.DAY_OF_MONTH, 1);
		return counter;
	}
}
