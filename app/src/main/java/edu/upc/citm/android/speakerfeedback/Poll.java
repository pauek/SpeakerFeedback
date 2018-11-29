package edu.upc.citm.android.speakerfeedback;

import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Poll {
    private String question;
    private List<String> options;
    private boolean open;
    private Date start, end;
    private List<Integer> results;

    static Poll errorPoll(String message) {
        Poll poll = new Poll();
        poll.question = "Error: " + message;
        poll.open = false;
        return poll;
    }

    Poll() {}

    Poll(String question) {
        this.question = question;
        this.options = new ArrayList<>();
        this.options.add("yes");
        this.options.add("no");
        this.open = true;
        this.start = new Date();
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public List<String> getOptions() {
        return options;
    }

    public String getOptionsAsString() {
        // Així se sumen molts strings a Java
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < options.size(); i++) {
            b.append(options.get(i));
            if (results != null) {
                b.append(" ");
                if (open) {
                    if (results.get(i) == null) {
                        b.append("0");
                    } else {
                        b.append(results.get(i));
                    }
                }
            }
            if (i < options.size()-1) {
                b.append("\n");
            }
        }
        return b.toString();
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    public List<Integer> getResults() {
        return results;
    }

    public void addVote(int option) {
        assert(results != null);
        if (option < results.size()) {
            results.set(option, results.get(option) + 1);
        } else {
            Log.e("SpeakerFeedback", "Error en el vot: opció fora de rang!");
        }
    }

    public void resetVotes() {
        results = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            results.add(0);
        }
    }
}
