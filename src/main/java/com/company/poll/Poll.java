package com.company.poll;

import com.company.User;

import java.util.ArrayList;
import java.util.List;

public class Poll {
    private String title;
    private User creator;
    private List<String> options;
    private List<Vote> votes;

    public Poll(String title, User creator, List<String> options) {
        this.title = title;
        this.creator = creator;
        this.options = options;

        votes = new ArrayList<>();
    }

    public boolean addVote(Vote vote) {
        if(vote.getVotedOption() < 0 || vote.getVotedOption() >= options.size()) {
            return false;
        }

        votes.add(vote);

        return true;
    }

    public List<Vote> getVotes() {
        return votes;
    }

    @Override
    public String toString() {
        int numberOfVotes = votes.size();

        StringBuilder representation = new StringBuilder("Poll '" + title + "'\n");

        for(String option: options) {
            long numberOfVotesForOption = votes.stream().filter(v -> v.getVotedOption() == options.indexOf(option)).count();
            representation.append("\tOption '").append(option).append("': ").append(numberOfVotesForOption).append(" / ").append(numberOfVotes).append("\n");
        }

        return representation.toString();
    }
}
