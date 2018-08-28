package dao.edge;

import dao.vertex.ElementV;
import dao.vertex.RefV;
import helper.StringHelper;

public class TokenE extends E {
    public enum NamePart{PREFIX, FIRSTNAME, MIDDLENAME, LASTNAME, SUFFIX}

    //region Fields
    private Byte order;
    private Boolean isAbbr;
    private Boolean isBeforeDot;
    private NamePart namePart;
    private Byte confidence;
    //endregion

    //region Getters & Setters
    /**
     * Gets order of token in the reference name
     *
     * @return value of order
     */
    public Byte getOrder() {
        return order;
    }

    public void setOrder(Byte order) {
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
     * Gets confidence of name's part
     *
     * @return value of confidence
     */
    public Byte getConfidence() {
        return confidence;
    }

    public void setConfidence(Byte confidence) {
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

    //endregion

    public TokenE(RefV inV, ElementV outV, String type, String weight) {
        super(inV, outV, type, weight);
        this.order = Byte.valueOf(weight);
        this.isAbbr = StringHelper.isAbbreviated(outV.getVal().length());
        this.isBeforeDot = StringHelper.isBeforeDot(outV.getVal().length(), inV.getVal(), order);
    }

    @Override
    public String toString() {
        return String.format("E[%s] %s -%s-> %s", super.getType(), super.getInV().getVal(), namePart, super.getOutV().getVal());
    }
}
