package logic.matching;

import dao.edge.TokenE;
import dao.vertex.ElementV;

import java.util.*;
import java.util.stream.Collectors;

import static dao.edge.TokenE.NamePart.FIRSTNAME;
import static dao.edge.TokenE.NamePart.LASTNAME;
import static dao.edge.TokenE.NamePart.MIDDLENAME;

public class MatchResult{

    //region Fields
    private List<TokenE> sortedTokenEs;
    private ClusterProfile clusterProfile;
    private List<Matched> matchedEntries;
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
     * Gets isMatched entries of cluster profile and the refV.
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
     * Gets cluster profile entries that is not isMatched with RefV.
     *
     * @return List of {@code ClusterProfile.Entry}
     */
    public List<ClusterProfile.Entry> getNotMatchedProfileEntries() {
        return clusterProfile.entries.stream().filter(e -> matchedEntries.stream().noneMatch(m -> m.profileEntry.equals(e)))
                .collect(Collectors.toList());
    }

    /**
     * Gets RefV tokenEs that is not isMatched with the cluster profile.
     *
     * @return List of TokenEs that does not isMatched with profile entries.
     */
    public List<TokenE> getNotMatchedTokenEs() {
        return sortedTokenEs.stream().filter(e -> matchedEntries.stream().noneMatch(m -> m.refTokenE.equals(e)))
                .collect(Collectors.toList());
    }

    /**
     * Gets all {@code TokenEs} in the isMatched refV sorted by token's order
     *
     * @return a List of TokenEs sorted by token's order
     */
    public List<TokenE> getSortedTokenEs() {
        return sortedTokenEs;
    }

    /**
     * @param nameParts set name parts of {@code sortedTokenEs} according to the indices of {@code nameParts} array.
     */
    public void setTokenEsNamesPart(TokenE.NamePart[] nameParts) {
        for (int i = 0, sortedTokenEsSize = sortedTokenEs.size(); i < sortedTokenEsSize; i++)
            sortedTokenEs.get(i).setNamePart(nameParts[i]);
    }

    /**
     * @return an array of {@code NamePart}s from tokenEs that Shifted to Left.
     */
    public TokenE.NamePart[] getShiftedLeftNameParts() {
        TokenE.NamePart[] nameParts = new TokenE.NamePart[sortedTokenEs.size()];
        boolean isShiftStarted = false;
        for (int i = 0, sortedTokenEsSize = sortedTokenEs.size(); i < sortedTokenEsSize; i++) {
            TokenE.NamePart part = sortedTokenEs.get(i).getNamePart();
            if(part == MIDDLENAME && sortedTokenEs.get(i+1).getNamePart() == LASTNAME)
                isShiftStarted = true;
            nameParts[i] = isShiftStarted ? part.nextRankedNamePart() : part;
        }
        return isShiftStarted? nameParts : null;
    }

    /**
     * @return an array of {@code NamePart}s from tokenEs that Shifted to Right.
     */
    public TokenE.NamePart[] getShiftedRightNameParts() {
        TokenE.NamePart[] nameParts = new TokenE.NamePart[sortedTokenEs.size()];
        boolean isShiftStarted = false;
        for (int i = sortedTokenEs.size() - 1; i >= 0; i--) {
            TokenE.NamePart part = sortedTokenEs.get(i).getNamePart();
            if(part == MIDDLENAME && sortedTokenEs.get(i-1).getNamePart() == FIRSTNAME)
                isShiftStarted = true;
            nameParts[i] = isShiftStarted ? part.previousRankedNamePart() : part;
        }
        return isShiftStarted? nameParts : null;
    }
    
    /**
     * @return an array of {@code NamePart}s from tokenEs that Shifted to Right.
     */
    public TokenE.NamePart[] getReversedFirstnameAndLastname() {
        TokenE.NamePart[] nameParts = new TokenE.NamePart[sortedTokenEs.size()];
        boolean hasLastname = false, hasFirstname = false;
        for (int i = 0, sortedTokenEsSize = sortedTokenEs.size(); i < sortedTokenEsSize; i++) {
            TokenE.NamePart part = sortedTokenEs.get(i).getNamePart();
            if(part == LASTNAME){
                nameParts[i] = FIRSTNAME;
                hasLastname = true;
            }
            if(part == FIRSTNAME){
                nameParts[i] = LASTNAME;
                hasFirstname = true;
            }
        }
        return hasLastname && hasFirstname? nameParts : null;
    }
    //endregion


    public MatchResult(ClusterProfile clusterProfile, List<TokenE> tokenEs) {
        this.clusterProfile = clusterProfile;
        this.sortedTokenEs = tokenEs.stream().sorted(Comparator.comparing(TokenE::getOrder)).collect(Collectors.toList());
        matchedEntries = new ArrayList<>(3);
    }


    //region Methods

    /**
     * check if the result shows that according to this result, the refV is consistent with profile or not?
     *
     * @return a boolean that is false if not consistent and true otherwise.
     */
    public boolean isConsistent() {
        return isConsistent(this.sortedTokenEs.stream().map(TokenE::getNamePart).toArray(TokenE.NamePart[]::new));
    }

    /**
     * check if the result shows that according to this result, the refV is consistent with profile or not?
     *
     * @return a boolean that is false if not consistent and true otherwise.
     */
    public boolean isConsistent(TokenE.NamePart[] parts) {
        for (ClusterProfile.Entry profileEntry : clusterProfile.getEntries()) {
            Matched matched = matchedEntries.stream().filter(e -> e.profileEntry == profileEntry)
                    .sorted(Comparator.comparing((Matched m) -> m.matchedV.getLevel()) // 1) isMatched in lower level is prior
                            // 2) if a namePart matches with multiple tokens, it consider the one that has the same namePart
                            .thenComparing((Matched e) -> e.profileEntry.getNamePart() != parts[e.refTokenE.getOrder()])
                    ).findFirst().orElse(null);
            if(matched == null) {
                if(profileEntry.getNamePart() == LASTNAME || profileEntry.getNamePart() == FIRSTNAME)
                    return false;
                if (getNotMatchedTokenEs().stream().filter(e -> parts[e.getOrder()] == profileEntry.getNamePart()).count() > 0)
                    return false; // if refV has any token with the same name's part, it must be isMatched.
            } else if(matched.hasEqualNamePart(parts)){
                if(profileEntry.getNamePart() == LASTNAME){
                    if(matched.getMatchedV().getLevel() > 2) return false;
                } else { // TODO: 26/08/2018 it is possible that two middle name is isMatched that one is abbr and other not
                    if(matched.isNonAbbrsMatchedInAbbrLevel())
                        return false;
                }
            } else {
                // TODO: 07/08/2018 update token types if increase consensus and cluster again
                if(profileEntry.getNamePart() == LASTNAME || profileEntry.getNamePart() == FIRSTNAME)
                    return false;
            }
        }
        return true;
    }

    public boolean canBecomeConsistent() {
        TokenE.NamePart[] shiftedLeftNameParts = this.getShiftedLeftNameParts();
        boolean isConsistent = shiftedLeftNameParts != null && this.isConsistent(shiftedLeftNameParts);
        if (isConsistent) {
            this.setTokenEsNamesPart(shiftedLeftNameParts);
            return true;
        }
//        TokenE.NamePart[] shiftedRightNameParts = this.getShiftedRightNameParts();
//        isConsistent = shiftedRightNameParts != null && this.isConsistent(shiftedRightNameParts);
//        if (isConsistent) {
//            this.setTokenEsNamesPart(shiftedRightNameParts);
//            return true;
//        }
        TokenE.NamePart[] reversed = this.getReversedFirstnameAndLastname();
        isConsistent = reversed != null && this.isConsistent(reversed);
        if (isConsistent) {
            this.setTokenEsNamesPart(reversed);
            return true;
        }
        return false;
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
         * Gets th Cluster Profile Entry that isMatched
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
         * Gets reference tokens that isMatched with profile entry.
         *
         * @return isMatched {@code TokenE}
         */
        public TokenE getRefTokenE() {
            return refTokenE;
        }

        public void setRefTokenE(TokenE refTokenE) {
            this.refTokenE = refTokenE;
        }

        /**
         * Gets ancestor {@code ElementV} that the profile and reference token isMatched in it.
         *
         * @return value of isMatched {@code ElementV}
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
         * @param parts array of NamePart that should be used as NamePart of {@code refTokenE}
         * @return true if {@code profileEntry} and {@code refTokenE} has equal name parts.
         */
        public boolean hasEqualNamePart(TokenE.NamePart[] parts) {
            return profileEntry != null && refTokenE != null &&
                    profileEntry.getNamePart() != null && refTokenE.getOrder() != null && parts.length > refTokenE.getOrder() &&
                    profileEntry.getNamePart() == parts[refTokenE.getOrder()];
        }

        /**
         * If a reference token and a clusterProfile entry are both Non-Abbreviated,
         * they should be isMatched in first level (token as is) or at second level (similar tokens).
         *
         * @return a boolean that Is the reference token and clusterProfile entry
         * are both non-abbreviated and isMatched in abbreviated level?
         */
        public boolean isNonAbbrsMatchedInAbbrLevel() {
            if(matchedV == null || refTokenE == null || profileEntry == null) return false;
            return matchedV.getLevel() == 3 && !refTokenE.getIsAbbr() && !profileEntry.getIsAbbr()
                    && !refTokenE.getIsBeforeDot() && !profileEntry.getIsBeforeDot();
        }

        /**
         * If a reference token and a clusterProfile entry are both Abbreviated,
         * they should be isMatched in first level (token as is).
         *
         * @return a boolean that Is the reference token and clusterProfile entry
         * are both abbreviated and is not isMatched in token level?
         */
        public boolean isAbbrsMatchedInNonTokenLevel() {
            if(matchedV == null || refTokenE == null || profileEntry == null) return false;
            return matchedV.getLevel() > 1 && refTokenE.getIsAbbr() && profileEntry.getIsAbbr();
        }

        /**
         * Check if profile entry is abbreviated, while isMatched refV tokenE's is not?
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
