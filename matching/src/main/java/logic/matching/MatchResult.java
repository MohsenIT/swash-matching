package logic.matching;

import dao.edge.TokenE;
import dao.vertex.ElementV;

import java.util.*;
import java.util.stream.Collectors;

import static dao.edge.TokenE.NamePart.FIRSTNAME;
import static dao.edge.TokenE.NamePart.LASTNAME;

public class MatchResult {

    //region Fields
    private ClusterProfile clusterProfile;
    private List<Entry> alignedEntries;
    private List<Entry> matchedEntries;
    private List<ClusterProfile.Entry> notMatchedProfileEntries;
    private List<TokenE> notMatchedTokenEs;
    //endregion


    //region Getters & Setters
    /**
     * Gets alignedEntries
     *
     * @return value of alignedEntries
     */
    public List<Entry> getAlignedEntries() {
        return alignedEntries;
    }

    public void buildAlignedEntries(List<ClusterProfile.Entry> profile, List<TokenE> tokenEs) {
        alignedEntries = new LinkedList<Entry>();
        for (int i = 0; i < profile.size(); i++) {
            ClusterProfile.Entry profileEntry = profile.get(i);
            Optional<Entry> matchedEntry = matchedEntries.stream().filter(e -> e.profileEntry == profileEntry).findAny();
            alignedEntries.add(matchedEntry.orElse(new Entry(profileEntry)));
            // TODO: 26/08/2018 complete it when matches is not crossed


        }
    }

    /**
     * Gets matched entries of cluster profile and the refV.
     *
     * @return List of {@code MatchResult.Entry}
     */
    public List<Entry> getMatchedEntries() {
        return matchedEntries;
    }

    public void setMatchedEntries(List<Entry> matchedEntries) {
        this.matchedEntries = matchedEntries;
    }

    public void addMatchedEntries(MatchResult.Entry entry) {
        this.matchedEntries.add(entry);
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
    //endregion


    public MatchResult(ClusterProfile clusterProfile) {
        this.clusterProfile = clusterProfile;
        matchedEntries = new ArrayList<>(3);
        notMatchedProfileEntries = new ArrayList<>(2);
        notMatchedTokenEs = new ArrayList<>(2);
    }

    /**
     * check if the result shows that according to this result, the refV is consistent with profile or not?
     *
     * @return a boolean that is false if not consistent and true otherwise.
     */
    public boolean isConsistent() {
        for (ClusterProfile.Entry profileEntry : clusterProfile.getEntries()) {

            Entry resEntry = matchedEntries.stream().filter(e -> e.profileEntry == profileEntry) // sort in order that if a namePart matches with multiple tokens, it consider the one that has the same namePart
                    .sorted(Comparator.comparing(e -> e.profileEntry.getNamePart() != e.refTokenE.getNamePart())).findFirst().orElse(null);
            if(resEntry == null) {
                if(profileEntry.getNamePart() == LASTNAME || profileEntry.getNamePart() == FIRSTNAME)
                    return false;
                if (notMatchedTokenEs.stream().filter(e -> e.getNamePart() == profileEntry.getNamePart()).count() > 0)
                    return false; // if refV has any token with the same name's part, it must be matched.
            } else if(resEntry.hasEqualNamePart()){
                if(profileEntry.getNamePart() == LASTNAME){
                    if(resEntry.getMatchedV().getLevel() > 2) return false;
                } else { // TODO: 26/08/2018 it is possible that two middle name is matched that one is abbr and other not
                    if(resEntry.isNonAbbrsMatchedInAbbrLevel()) return false;
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


    public static class Entry{
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

        public Entry(ClusterProfile.Entry profileEntry, TokenE refTokenE, ElementV matchedV) {
            this.profileEntry = profileEntry;
            this.refTokenE = refTokenE;
            this.matchedV = matchedV;
        }

        public Entry(ClusterProfile.Entry profileEntry) {
            this.profileEntry = profileEntry;
        }

        public Entry(TokenE refTokenE) {
            this.refTokenE = refTokenE;
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
            return String.format("Result.Entry{profileEntry={%s: %s}, refTokenE=%s, matchedV=%s}"
                    , profileEntry.getNamePart(), profileEntry.getElementV(), refTokenE, matchedV);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof MatchResult.Entry)) return false;
            Entry e = (Entry) o;
            return this.matchedV == e.matchedV && this.profileEntry == e.profileEntry && this.refTokenE == e.refTokenE;
        }
    }
}
