package com.company.poll;

import com.company.User;

class Vote {
    private User voter;
    private int votedOption;

    public Vote(User voter, int votedOption) {
        this.voter = voter;
        this.votedOption = votedOption;
    }

    public int getVotedOption() {
        return votedOption;
    }

    public User getVoter() {
        return voter;
    }

    @Override
    public String toString() {
        return "User '" + voter.getName() + "' voted for option nr. " + votedOption;
    }
}
