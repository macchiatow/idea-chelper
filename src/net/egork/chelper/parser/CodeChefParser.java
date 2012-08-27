package net.egork.chelper.parser;

import com.intellij.openapi.util.IconLoader;
import net.egork.chelper.checkers.TokenChecker;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.Test;
import net.egork.chelper.util.FileUtilities;
import org.apache.commons.lang.StringEscapeUtils;

import javax.swing.*;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Egor Kulikov (egor@egork.net)
 */
public class CodeChefParser implements Parser {
    private final static String EASY_ID = "problems/easy";
    private final static String MEDIUM_ID = "problems/medium";
    private final static String HARD_ID = "problems/hard";
    private final static String CHALLENGE_ID = "challenge/easy";
    private final static String PEER_ID = "problems/extcontest";
    private final static List<String> SPECIAL = Arrays.asList(EASY_ID, MEDIUM_ID, HARD_ID, CHALLENGE_ID, PEER_ID);
    private ParseContestTask task;

    public Icon getIcon() {
		return IconLoader.getIcon("/icons/codechef.png");
	}

	public String getName() {
		return "CodeChef";
	}

    public Collection<Description> getContests(DescriptionReceiver receiver) {
        String mainPage;
        try {
            mainPage = FileUtilities.getWebPageContent("http://www.codechef.com/contests");
        } catch (IOException e) {
            return Collections.emptyList();
        }
        StringParser parser = new StringParser(mainPage);
        List<Description> contests = new ArrayList<Description>();
        try {
            parser.advance(true, "<h3>Future Contests</h3>");
            parser = new StringParser(parser.advance(false, "<h3>Past Contests</h3>"));
            while (parser.advanceIfPossible(true, "<tr ><td >") != null) {
                String id = parser.advance(false, "</td>");
                parser.advance(true, "<a href=\"");
                parser.advance(true, "\">");
                String name = parser.advance(false, "</a>");
                contests.add(new Description(id, name));
            }
        } catch (ParseException e) {
            contests.addAll(buildSpecial());
            return contests;
        }
        contests.addAll(buildSpecial());
        return contests;
    }

    private Collection<Description> buildSpecial() {
        List<Description> special = new ArrayList<Description>();
        special.add(new Description(EASY_ID, "Easy problems"));
        special.add(new Description(MEDIUM_ID, "Medium problems"));
        special.add(new Description(HARD_ID, "Hard problems"));
        special.add(new Description(CHALLENGE_ID, "Challenge problems"));
        special.add(new Description(PEER_ID, "External contests problems"));
        return special;
    }

    public Collection<Description> parseContest(String id, DescriptionReceiver receiver) {
        if (SPECIAL.contains(id)) {
            new Thread(task = new ParseContestTask(id, receiver)).start();
            return Collections.emptyList();
        }
        return parseContestImpl(id, 25, null);
	}

    private Collection<Description> parseContestImpl(String id, int retries, ParseContestTask pct) {
        String mainPage = null;
        for (int i = 0; i < retries; i++) {
            try {
                mainPage = FileUtilities.getWebPageContent("http://www.codechef.com/" + id);
                break;
            } catch (IOException ignored) {
                if (pct != null && pct.stopped)
                    return Collections.emptyList();
            }
        }
        if (mainPage == null)
            return Collections.emptyList();
        List<Description> tasks = new ArrayList<Description>();
        StringParser parser = new StringParser(mainPage);
        try {
            parser.advance(true, "Accuracy</a></th>");
            parser = new StringParser(parser.advance(false, "</tbody>"));
        } catch (ParseException e) {
            return Collections.emptyList();
        }
        if (SPECIAL.contains(id))
            id = "";
        while (true) {
            try {
                parser.advance(true, id + "/problems/");
String taskID;
if (id.length() == 0)
taskID = parser.advance(false, "\"");
else
taskID = id + " " + parser.advance(false, "\"");
parser.advance(true, "<b>");
String name = parser.advance(false, "</b>");
tasks.add(new Description(taskID, name));
            } catch (ParseException e) {
                break;
            }
        }
        return tasks;
    }

    public Task parseTask(String id) {
		String[] tokens = id.split(" ");
		if (tokens.length > 2 || tokens.length == 0)
			return null;
		String url;
		if (tokens.length == 2)
			url = "http://www.codechef.com/" + tokens[0] + "/problems/" + tokens[1];
		else
			url = "http://www.codechef.com/problems/" + tokens[0];
		String text;
		try {
			text = FileUtilities.getWebPageContent(url);
		} catch (IOException e) {
			return null;
		}
		StringParser parser = new StringParser(text);
		Pattern pattern = Pattern.compile(".*<p>.*</p>.*", Pattern.DOTALL);
		try {
			parser.advance(false, "<div class=\"prob\">");
			parser.advance(true, "<h1>");
			String taskID = getTaskID(parser.advance(false, "</h1>"));
			parser.dropTail("<table cellspacing=\"0\" cellpadding=\"0\" align=\"left\">");
			List<Test> tests = new ArrayList<Test>();
			int index = 0;
			while (true) {
				try {
					parser.advance(true, "Input", "Sample input", "Sample Input");
					if (parser.length() != 0 && parser.charAt(0) == ':')
						parser.advance(1);
					String input = parser.advance(true, "Output", "Sample output", "Sample Output");
					if (parser.length() != 0 && parser.charAt(0) == ':')
						parser.advance(1);
					String output = parser.advance(false, "Input", "Sample input", "Sample Input", "<b>",
						"<h", "</div>", "<p>");
					if (pattern.matcher(input).matches() || input.contains("</p><p>"))
						continue;
					input = dropTags(input).replace("<br />\n", "\n").replace("<br />", "\n");
					output = dropTags(output).replace("<br />\n", "\n").replace("<br />", "\n");
					if (input.contains("<") || output.contains("<"))
						continue;
					tests.add(new Test(StringEscapeUtils.unescapeHtml(input), StringEscapeUtils.unescapeHtml(output),
						index++));
				} catch (ParseException e) {
					break;
				}
			}
            return new Task(taskID, null, StreamConfiguration.STANDARD, StreamConfiguration.STANDARD,
                    tests.toArray(new Test[tests.size()]), null, "-Xmx64M", null, taskID,
                    TokenChecker.class.getCanonicalName(), "", new String[0], null, null, true, null, null);
		} catch (ParseException e) {
			return null;
		}
	}

    public void stopAdditionalContestSending() {
    }

    public void stopAdditionalTaskSending() {
        if (task != null)
            task.stopped = true;
        task = null;
    }

    private String dropTags(String s) {
		int bracket = 0;
		while (s.length() != 0) {
			char c = s.charAt(0);
			if (c == '<')
				bracket++;
			else if (bracket == 0 && c != ' ' && c != '\n')
				break;
			else if (c == '>')
				bracket--;
			s = s.substring(1);
		}
		while (s.length() != 0) {
			char c = s.charAt(s.length() - 1);
			if (c == '>')
				bracket++;
			else if (bracket == 0 && c != ' ' && c != '\n')
				break;
			else if (c == '<')
				bracket--;
			s = s.substring(0, s.length() - 1);
		}
		return s + "\n";
	}

	private String getTaskID(String title) {
		boolean shouldBeCapital = true;
		StringBuilder id = new StringBuilder();
		for (int i = 0; i < title.length(); i++) {
			if (Character.isLetter(title.charAt(i))) {
				if (shouldBeCapital) {
					shouldBeCapital = false;
					id.append(Character.toUpperCase(title.charAt(i)));
				} else
					id.append(title.charAt(i));
			} else if (title.charAt(i) == ' ')
				shouldBeCapital = true;
		}
		return id.toString();
	}

    private class ParseContestTask implements Runnable {
        private boolean stopped;
        private String id;
        private DescriptionReceiver receiver;

        public ParseContestTask(String id, DescriptionReceiver receiver) {
            this.id = id;
            this.receiver = receiver;
        }

        public void run() {
            Collection<Description> tasks = parseContestImpl(id, Integer.MAX_VALUE, this);
            if (!stopped)
                receiver.receiveAdditionalDescriptions(tasks);
        }
    }
}