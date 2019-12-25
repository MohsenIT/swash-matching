package logic.matching;

import com.koloboke.collect.map.hash.HashObjObjMaps;
import dao.edge.E;
import dao.edge.TokenE;
import dao.edge.TokenE.NamePart;
import dao.vertex.ElementV;
import dao.vertex.RefV;
import dao.vertex.V;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;

public class ClusterProfile {

    //region Fields
    List<Entry> entries;
    //endregion


    //region Getters & Setters
    /**
     * Gets ClusterProfile entries
     *
     * @return List of value of ClusterProfile.Entry
     */
    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

    public void addEntry(Entry entry) {
        this.entries.add(entry);
    }

    public void addEntry(Entry entry, Integer index) {
        entry.setOrder(index);
        this.entries.stream().filter(e -> e.getOrder() >= index).forEach(Entry::incOrder);
        this.entries.add(index, entry);
    }
    //endregion


    public ClusterProfile() {
        this.entries = new ArrayList<>(4);
    }


    //region Methods
    public MatchResult match(RefV refV) {
        Map<ElementV, Set<TokenE>> refMap = refV.getTokenEs().stream().collect(groupingBy(TokenE::getOutV, mapping(Function.identity(), toSet())));
        Map<ElementV, Set<ClusterProfile.Entry>> profileMap = entries.stream().collect(groupingBy(Entry::getElementV, mapping(Function.identity(), toSet())));

        MatchResult result = new MatchResult(this, refV.getTokenEs());
        for (int i = 1; i <= V.Type.maxLevel; i++) {
            refMap = outElementVsAtLeast(refMap, i);
            profileMap = outElementVsAtLeast(profileMap, i);

            List<MatchResult.Matched> matchedEntriesToRemove = new ArrayList<>();
            for (Map.Entry<ElementV, Set<TokenE>> r : refMap.entrySet()) {
                if(profileMap.containsKey(r.getKey()))
                    profileMap.get(r.getKey()).forEach(entry -> r.getValue().forEach(tokenE -> {
                        MatchResult.Matched matched = new MatchResult.Matched(entry, tokenE, r.getKey());
                        if(!matched.isNonAbbrsMatchedInAbbrLevel() && !matched.isAbbrsMatchedInNonTokenLevel()) {
                            result.addMatchedEntries(matched);
                            if (entry.getNamePart() == tokenE.getNamePart()) // If not, may be isMatched in upper level
                                matchedEntriesToRemove.add(matched);
                        }
                    }));
            }
            // elementVs is removed out of above loops to does not change Maps during iteration
            // if an entry contains more than 1 element on its set, remove only the same element (not whole entry)
            for (MatchResult.Matched me : matchedEntriesToRemove) {
                Set<TokenE> tokenEs = refMap.get(me.getMatchedV());
                if(tokenEs != null && tokenEs.size() > 1) tokenEs.remove(me.getRefTokenE()); else refMap.remove(me.getMatchedV());
                Set<Entry> profEs = profileMap.get(me.getMatchedV());
                if(profEs != null && profEs.size() > 1) profEs.remove(me.getProfileEntry()); else profileMap.remove(me.getMatchedV());
            }
        }
        return result;
    }

    public void merge(MatchResult matchResult) {
        matchResult.getNotMatchedTokenEs().stream().map(Entry::new).forEach(entry -> {
            Integer index = matchResult.getMatchedEntries().stream()
                    .filter(e -> e.getRefTokenE().getOrder() < entry.getOrder())
                    .mapToInt(e -> e.getProfileEntry().getOrder()).max().orElse(0) + 1;
            addEntry(entry, index);
        });

        // TODO: 27/08/2018 Is this merge is valid? We should change 'Ali A Raeesi' to 'Ali Akbar Raeesi'?
        matchResult.getMatchedEntries().stream().filter(MatchResult.Matched::isProfileAbbrAndRefNonAbbr)
                .forEach(matched -> matched.setProfileEntry(new Entry(matched.getRefTokenE())));

        // TODO: 27/08/2018 if isMatched on 2nd level?
    }

    /**
     * traverse out vertices until all of them at least reached to {@code minLevel}.
     * if the level of a vertex is greater than {@code minLevel}, this vertex does not traversed.
     *
     * @param vMap a Map of {@code ElementV} to a collection of objects.
     * @param minLevel minimum level of output {@code ElementV}s.
     * @param <T> A generic type of my parameter
     * @return set of vertices in the minLevel
     */
    public static <T> Map<ElementV, Set<T>> outElementVsAtLeast(Map<ElementV, Set<T>> vMap, int minLevel) {
        Map<ElementV, Set<T>> resultMap = HashObjObjMaps.newMutableMap(vMap.size());
        for (Map.Entry<ElementV, Set<T>> entry : vMap.entrySet()) {
            int currentLevel = entry.getKey().getType().getLevel();
            if(currentLevel >= minLevel)
                resultMap.put(entry.getKey(), entry.getValue());
            else {
                Set<ElementV> vs = Collections.singleton(entry.getKey());
                while (currentLevel++ < minLevel){
                    vs = vs.stream().flatMap(v -> v.getOutNextLevelV().stream()).map(ElementV.class::cast).collect(toSet());
                }
                for (ElementV v : vs) {
                    if(resultMap.containsKey(v))
                        resultMap.get(v).addAll(entry.getValue());
                    else
                        resultMap.put(v, entry.getValue());
                }
            }
        }
        return resultMap;
    }

    @Override
    public String toString() {
        return String.format("%s [entries=%s]", entries.size(), entries.stream().map(Object::toString).collect(Collectors.joining(", ")));
    }

    // TODO: 22/08/2018 implement Shift and reverse for profile.
    //endregion


    public static class Entry{
        //region Fields
        private ElementV elementV;
        private Boolean isAbbr;
        private Boolean isBeforeDot;
        private NamePart namePart;
        private Integer order;
        //endregion

        //region Getters & Setters
        /**
         * Gets the Element vertex that owns a profile.
         *
         * @return value of elementV
         */
        public ElementV getElementV() {
            return elementV;
        }

        public void setElementV(ElementV elementV) {
            this.elementV = elementV;
        }

        /**
         * Gets is the entry Abbreviated or not?
         *
         * @return boolean value of isAbbreviated
         */
        public Boolean getIsAbbr() {
            return isAbbr;
        }

        public void setIsAbbr(Boolean abbr) {
            isAbbr = abbr;
        }

        /**
         * Is a dot exists after name's token or not?
         *
         * @return <tt>true</tt> if exist and otherwise <tt>false</tt>
         */
        public Boolean getIsBeforeDot() {
            return isBeforeDot;
        }

        public void setIsBeforeDot(Boolean beforeDot) {
            isBeforeDot = beforeDot;
        }

        /**
         * Gets namePart of Element vertex.
         *
         * @return value of namePart that contains firstname, lastname and etc.
         */
        public NamePart getNamePart() {
            return namePart;
        }

        public void setNamePart(NamePart namePart) {
            this.namePart = namePart;
        }

        /**
         * Gets order of elementV in the profile name.
         *
         * @return Integer value of order
         */
        public Integer getOrder() {
            return order;
        }

        public void setOrder(Integer order) {
            this.order = order;
        }

        public void incOrder() {
            this.order++;
        }
        //endregion

        public Entry(TokenE tokenE) {
            this.elementV = tokenE.getOutV();
            this.isAbbr = tokenE.getIsAbbr();
            this.isBeforeDot = tokenE.getIsBeforeDot();
            this.namePart = tokenE.getNamePart();
            this.order = tokenE.getOrder();
        }

        @Override
        public String toString() {
            return String.format("%s {%s%s, order=%s}", elementV, isAbbr?"ABBR ":"", namePart, order);
        }
    }

}
