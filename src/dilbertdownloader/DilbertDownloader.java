package dilbertdownloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;

/**
 * 
 * @author Eugene Susla
 */
public class DilbertDownloader {

	private static final ResourceBundle config = ResourceBundle
			.getBundle("dilbertdownloader.config");

	private static final String beginSequence = config
			.getString("beginSequence");
	private static final String endSequence = config.getString("endSequence");
	private static final String targetFolder = config.getString("targetFolder");
	private static final String baseUrl = config.getString("baseUrl");
	private static final String domainUrl = config.getString("domainUrl");

	private static volatile String input = "";

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) {
		InputStreamReader converter = new InputStreamReader(System.in);
		BufferedReader in = new BufferedReader(converter);

		Logger.getLogger(DilbertDownloader.class.getName()).info(
				"Downloading lastest comics:");
		downloadLastest();

		Thread thread = new Thread(new Runnable() {
			public void run() {
				Logger.getLogger(DilbertDownloader.class.getName()).info(
						"Downloading comics:");
				Calendar counter = findFirstPresentComic();
				int comicsDownloaded = 0;
				while (!Thread.interrupted()) {
					downloadComic(counter, false, true);
					counter.add(Calendar.DAY_OF_MONTH, -1);
					comicsDownloaded++;
				}
				Logger.getLogger(DilbertDownloader.class.getName()).info(
						"Finished downloading. Comics downloaded: "
								+ comicsDownloaded);
			}
		});
		thread.start();
		input = "";
		while (input.isEmpty()) {
			try {
				input = in.readLine();
			} catch (IOException ex) {
				Logger.getLogger(DilbertDownloader.class.getName()).error(null,
						ex);
			}
		}
		// FIXME exiting mechanism broken
		thread.interrupt();
	}

	/**
	 * 
	 * @param date
	 *            yyyy-mm-dd
	 * @return URL to png
	 */
	private static String getURL(String date) {
		String html;
		try {
			html = util.net.Http.getResponsePart(baseUrl + date + "/",
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

	/**
	 * 
	 * @param date
	 *            yyyy-mm-dd
	 * @return
	 */
	private static boolean isComicForDateDownloaded(String date) {
		String file = targetFolder + "\\" + date.substring(0, 4) + "\\" + date
				+ ".gif";
		// TODO Investigate for a better way to check if a file exists w/o
		// creating a new object
		return new File(file).exists();
	}

	private static boolean isComicForDateDownloaded(Calendar date) {
		return isComicForDateDownloaded(calendarToString(date));
	}

	private static boolean downloadComic(String date,
			boolean checkForAlreadyDownloaded, boolean verbose) {
		String year = date.substring(0, 4);
		String folderName = targetFolder + "\\" + year;
		String fileName = folderName + "\\" + date + ".gif";
		// TODO Investigate for a better way to check if a file exists w/o
		// creating a new object
		if (checkForAlreadyDownloaded && (new File(fileName)).exists()) {
			return false;
		}
		String url = getURL(date);
		if (url == null) {
			Logger.getLogger(DilbertDownloader.class.getName()).warn(
					"There seems to be no comic for date " + date);
			return false;
		}
		Logger.getLogger(DilbertDownloader.class.getName()).debug(
				"Download url: " + url);
		try {
			(new File(folderName)).mkdir();
			util.net.Http.writeResponseToFile(url, fileName);
			if (verbose)
				Logger.getLogger(DilbertDownloader.class.getName()).info(
						"Comic downloaded for date: " + date);
			return true;
		} catch (IOException ex) {
			Logger.getLogger(DilbertDownloader.class.getName()).error(
					"Error downloading comic for date " + date + " : ", ex);
			return false;
		}
	}

	private static boolean downloadComic(Calendar date,
			boolean checkForAlreadyDownloaded, boolean verbose) {
		return downloadComic(calendarToString(date), checkForAlreadyDownloaded,
				verbose);
	}

	// TODO Investigate if there is a better way to output calendar
	private static String calendarToString(Calendar cal) {
		String result = "" + cal.get(Calendar.YEAR) + "-"
				+ String.format("%02d", cal.get(Calendar.MONTH) + 1) + "-"
				+ String.format("%02d", cal.get(Calendar.DAY_OF_MONTH));
		return result;
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
