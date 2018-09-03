package logic.matching;

import dao.edge.TokenE;
import dao.vertex.ElementV;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dao.edge.TokenE.NamePart.FIRSTNAME;
import static dao.edge.TokenE.NamePart.LASTNAME;

public class MatchResult{

    //region Fields
    private List<TokenE> sortedTokenEs;
    private ClusterProfile clusterProfile;
    private List<Matched> matchedEntries;
    private List<ClusterProfile.Entry> notMatchedProfileEntries;
    private List<TokenE> notMatchedTokenEs;
    //endregion


    //region Getters & Setters

    /**
     * Gets clusterProfile that this class is the result of its matching
     *
     * @return value of clusterProfile
     */
    public ClusterProfile getClusterProfile() {
        return clusterProfile;
    }

    public void setClusterProfile(ClusterProfile clusterProfile) {
        this.clusterProfile = clusterProfile;
    }

    /**
     * Gets matched entries of cluster profile and the refV.
     *
     * @return List of {@code MatchResult.Matched}
     */
    public List<Matched> getMatchedEntries() {
        return matchedEntries;
    }

    public void setMatchedEntries(List<Matched> matchedEntries) {
        this.matchedEntries = matchedEntries;
    }

    public void addMatchedEntries(Matched matched) {
        this.matchedEntries.add(matched);
    }

    /**
     * Gets cluster profile entries that is not matched with RefV.
     *
     * @return List of {@code ClusterProfile.Entry}
     */
    public List<ClusterProfile.Entry> getNotMatchedProfileEntries() {
        return notMatchedProfileEntries;
    }

    public void setNotMatchedProfileEntries(List<ClusterProfile.Entry> notMatchedProfileEntries) {
        this.notMatchedProfileEntries = notMatchedProfileEntries;
    }

    public void addNotMatchedProfileEntries(ClusterProfile.Entry entry) {
        this.notMatchedProfileEntries.add(entry);
    }

    /**
     * Gets RefV tokens that is not matched with cluster profile.
     *
     * @return value of notMatchedTokenEs
     */
    public List<TokenE> getNotMatchedTokenEs() {
        return notMatchedTokenEs;
    }

    public void setNotMatchedTokenEs(List<TokenE> notMatchedTokenEs) {
        this.notMatchedTokenEs = notMatchedTokenEs;
    }

    public void addNotMatchedTokenEs(TokenE tokenE) {
        this.notMatchedTokenEs.add(tokenE);
    }

    /**
     * Gets all {@code TokenEs} in the matched refV sorted by token's order
     *
     * @return a List of TokenEs sorted by token's order
     */
    public List<TokenE> getSortedTokenEs() {
        return sortedTokenEs;
    }

    public void setSortedTokenEs() {
        sortedTokenEs = Stream.concat(notMatchedTokenEs.stream(), matchedEntries.stream().map(Matched::getRefTokenE))
                .distinct().sorted(Comparator.comparing(TokenE::getOrder)).collect(Collectors.toList());
    }

    public MatchResult shiftLeftNamesPart() {
        TokenE lastMiddle = sortedTokenEs.stream().filter(TokenE::isMiddleName)
                .reduce((first, second) -> second).orElse(null);
        if(lastMiddle != null) {
            sortedTokenEs.stream().filter(e -> e.getNamePart().getRank() > 3).forEach(TokenE::incNamePartRank);
            lastMiddle.setNamePart(LASTNAME);
        }
        return this;
    }
    //endregion


    public MatchResult(ClusterProfile clusterProfile, List<TokenE> tokenEs) {
        this.clusterProfile = clusterProfile;
        this.sortedTokenEs = tokenEs;
        matchedEntries = new ArrayList<>(3);
        notMatchedProfileEntries = new ArrayList<>(2);
        notMatchedTokenEs = new ArrayList<>(2);
    }


    //region Methods
    /**
     * Copy the current object by reference instead refTokenE that create new object from it.
     * This method in check NamePart change in order to the original object did not affect.
     *
     * @return a new MatchResult object
     */
    public MatchResult copyWithNewTokenEs() {
        MatchResult result = new MatchResult(this.getClusterProfile(), this.sortedTokenEs);
        result.setNotMatchedProfileEntries(this.notMatchedProfileEntries);
        result.setMatchedEntries(this.matchedEntries.stream().map(Matched::copyWithNewTokenE).collect(Collectors.toList()));
        result.setNotMatchedTokenEs(this.notMatchedTokenEs.stream().map(TokenE::clone).collect(Collectors.toList()));
        result.setSortedTokenEs();
        return result;
    }

    /**
     * check if the result shows that according to this result, the refV is consistent with profile or not?
     *
     * @return a boolean that is false if not consistent and true otherwise.
     */
    public boolean isConsistent() {
        for (ClusterProfile.Entry profileEntry : clusterProfile.getEntries()) {

            Matched matched = matchedEntries.stream().filter(e -> e.profileEntry == profileEntry) // sort in order that if a namePart matches with multiple tokens, it consider the one that has the same namePart
                    .sorted(Comparator.comparing(e -> e.profileEntry.getNamePart() != e.refTokenE.getNamePart())).findFirst().orElse(null);
            if(matched == null) {
                if(profileEntry.getNamePart() == LASTNAME || profileEntry.getNamePart() == FIRSTNAME)
                    return false;
                if (notMatchedTokenEs.stream().filter(e -> e.getNamePart() == profileEntry.getNamePart()).count() > 0)
                    return false; // if refV has any token with the same name's part, it must be matched.
            } else if(matched.hasEqualNamePart()){
                if(profileEntry.getNamePart() == LASTNAME){
                    if(matched.getMatchedV().getLevel() > 2) return false;
                } else { // TODO: 26/08/2018 it is possible that two middle name is matched that one is abbr and other not
                    if(matched.isNonAbbrsMatchedInAbbrLevel()) return false;
                }
            } else {
                // TODO: 26/08/2018 check if fname and lname is reversed or not?
                // TODO: 07/08/2018 update token types if increase consensus and cluster again
                if(profileEntry.getNamePart() == LASTNAME || profileEntry.getNamePart() == FIRSTNAME)
                    return false;
            }
        }
        return true;
    }

    public boolean canBecomeConsistent() {
        MatchResult r = copyWithNewTokenEs().shiftLeftNamesPart();
        if(r.isConsistent())
            this.shiftLeftNamesPart();
        return true;
    }

    //endregion

    public static class Matched implements Cloneable{
        //region Fields
        private ClusterProfile.Entry profileEntry;
        private TokenE refTokenE;
        private ElementV matchedV;
        //endregion

        //region Getters & Setters
        /**
         * Gets th Cluster Profile Entry that matched
         *
         * @return value of {@code ClusterProfile.Entry}
         */
        public ClusterProfile.Entry getProfileEntry() {
            return profileEntry;
        }

        public void setProfileEntry(ClusterProfile.Entry profileEntry) {
            this.profileEntry = profileEntry;
        }

        /**
         * Gets reference tokens that matched with profile entry.
         *
         * @return matched {@code TokenE}
         */
        public TokenE getRefTokenE() {
            return refTokenE;
        }

        public void setRefTokenE(TokenE refTokenE) {
            this.refTokenE = refTokenE;
        }

        /**
         * Gets ancestor {@code ElementV} that the profile and reference token matched in it.
         *
         * @return value of matched {@code ElementV}
         */
        public ElementV getMatchedV() {
            return matchedV;
        }

        public void setMatchedV(ElementV matchedV) {
            this.matchedV = matchedV;
        }
        //endregion+

        public Matched(ClusterProfile.Entry profileEntry, TokenE refTokenE, ElementV matchedV) {
            this.profileEntry = profileEntry;
            this.refTokenE = refTokenE;
            this.matchedV = matchedV;
        }

        public Matched(ClusterProfile.Entry profileEntry) {
            this.profileEntry = profileEntry;
        }

        public Matched(TokenE refTokenE) {
            this.refTokenE = refTokenE;
        }

        /**
         * Copy the current object by reference instead refTokenE that create new object from it
         *
         * @return a new Matched object
         */
        public Matched copyWithNewTokenE(){
            return new Matched(this.profileEntry, this.refTokenE.clone(), this.matchedV);
        }

        public boolean hasEqualNamePart() {
            return profileEntry != null && refTokenE != null
                    && profileEntry.getNamePart() != null && refTokenE.getNamePart() != null
                    && profileEntry.getNamePart() == refTokenE.getNamePart();
        }

        /**
         * If a reference token and a clusterProfile entry are both Non-Abbreviated,
         * they should be matched in first level (token as is) or at second level (similar tokens).
         *
         * @return a boolean that Is the reference token and clusterProfile entry
         * are both non-abbreviated and matched in abbreviated level?
         */
        public boolean isNonAbbrsMatchedInAbbrLevel() {
            if(matchedV == null || refTokenE == null || profileEntry == null) return false;
            return matchedV.getLevel() == 3 && !refTokenE.getIsAbbr() && !profileEntry.getIsAbbr()
                    && !refTokenE.getIsBeforeDot() && !profileEntry.getIsBeforeDot();
        }

        /**
         * If a reference token and a clusterProfile entry are both Abbreviated,
         * they should be matched in first level (token as is).
         *
         * @return a boolean that Is the reference token and clusterProfile entry
         * are both abbreviated and is not matched in token level?
         */
        public boolean isAbbrsMatchedInNonTokenLevel() {
            if(matchedV == null || refTokenE == null || profileEntry == null) return false;
            return matchedV.getLevel() > 1 && refTokenE.getIsAbbr() && profileEntry.getIsAbbr();
        }

        /**
         * Check if profile entry is abbreviated, while matched refV tokenE's is not?
         *
         * @return a boolean value, {@code true} if the above condition exist and {@code false} otherwise
         */
        public boolean isProfileAbbrAndRefNonAbbr() {
            if(matchedV == null || refTokenE == null || profileEntry == null) return false;
            return profileEntry.getIsAbbr() && !refTokenE.getIsAbbr();
        }

        @Override
        public String toString() {
            return String.format("Result.Matched{profileEntry={%s: %s}, refTokenE=%s, matchedV=%s}"
                    , profileEntry.getNamePart(), profileEntry.getElementV(), refTokenE, matchedV);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof Matched)) return false;
            Matched e = (Matched) o;
            return this.matchedV == e.matchedV && this.profileEntry == e.profileEntry && this.refTokenE == e.refTokenE;
        }
    }
}
