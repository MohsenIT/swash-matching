package dao.edge;

import dao.vertex.ElementV;
import dao.vertex.RefV;
import dao.vertex.V;
import helper.StringHelper;

public class TokenE extends E implements Cloneable{
    public enum NamePart{
        PREFIX(1), FIRSTNAME(2), MIDDLENAME(3), LASTNAME(4), SUFFIX(5);

        private int rank;

        //region Getters & Setters
        /**
         * Gets default rank of name's part
         *
         * @return int value of rank
         */
        public int getRank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }
        //endregion

        NamePart(int rank) {
            this.rank = rank;
        }

        public NamePart nextRankedNamePart() {
            if(this.rank == 5) return SUFFIX;
            return NamePart.values()[this.rank];
        }

        public NamePart previousRankedNamePart() {
            if(this.rank == 1) return PREFIX;
            return NamePart.values()[this.rank-2];
        }
    }


    //region Fields
    private Integer order;
    private Boolean isAbbr;
    private Boolean isBeforeDot;
    private NamePart namePart;
    private Integer confidence;
    //endregion


    //region Getters & Setters
    /**
     * Gets rank of token in the reference name
     *
     * @return value of rank
     */
    public Integer getOrder() {
        return order;
    }

    /**
     * @param order zero indexed order of this token in {@code refV}
     */
    public void setOrder(Integer order) {
        this.order = order;
    }

    /**
     * Is the token abbreviated or not?
     *
     * @return boolean value isAbbreviation
     */
    public Boolean getIsAbbr() {
        return isAbbr;
    }

    public void setIsAbbr(Boolean isAbbr) {
        this.isAbbr = isAbbr;
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
     * Gets predicted name's part of token
     *
     * @return value of name's part: PREFIX, FIRSTNAME, MIDDLENAME, LASTNAME or SUFFIX
     */
    public NamePart getNamePart() {
        return namePart;
    }

    public void setNamePart(NamePart namePart) {
        this.namePart = namePart;
    }

    /**
     * Increase the rank of namePart, for example if namepart is LASTNAME change it to SUFFIX
     */
    public void incNamePartRank() {
        namePart = namePart.nextRankedNamePart();
    }

    /**
     * Decrease the rank of namePart, for example if namepart is FIRSTNAME change it to PREFIX
     */
    public void decNamePartRank() {
        namePart = namePart.previousRankedNamePart();
    }




    /**
     * Gets confidence of name's part
     *
     * @return value of confidence
     */
    public Integer getConfidence() {
        return confidence;
    }

    public void setConfidence(Integer confidence) {
        this.confidence = confidence;
    }

    @Override
    public ElementV getOutV() {
        return (ElementV) super.getOutV();
    }

    @Override
    public RefV getInV() {
        return (RefV) super.getInV();
    }

    public boolean isMiddleName(){
        return namePart == NamePart.MIDDLENAME;
    }

    //endregion


    public TokenE(RefV inV, ElementV outV, String type, String weight) {
        super(inV, outV, type, weight);
        this.order = Integer.valueOf(weight) - 1;
        this.isAbbr = StringHelper.isAbbreviated(outV.getVal().length());
        this.isBeforeDot = StringHelper.isBeforeDot(outV.getVal().length(), inV.getVal(), order);
    }

    public TokenE(TokenE e) {
        super(e.getInV(), e.getOutV(), e.getType(), e.getWeight());
        this.order = e.getOrder();
        this.isAbbr = e.getIsAbbr();
        this.isBeforeDot = e.getIsBeforeDot();
        this.namePart = e.getNamePart();
        this.confidence = e.getConfidence();
    }


    @Override
    public String toString() {
        return String.format("E[%s] %s -%s-> %s", super.getType(), super.getInV().getVal(), namePart, super.getOutV().getVal());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof TokenE)) return false;
        TokenE e = (TokenE) o;
        return super.getInV().equals(e.getInV()) && super.getOutV().equals(e.getOutV()) && this.order.equals(e.order);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + super.getInV().hashCode();
        result = 31 * result + super.getOutV().hashCode();
        result = 31 * result + this.order;
        return result;
    }
}
