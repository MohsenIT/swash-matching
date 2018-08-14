package ds;

import helper.StringHelper;

public class TokenE extends E {
    public enum NamePart{PREFIX, FIRSTNAME, MIDDLENAME, LASTNAME, SUFFIX}

    //region Fields
    private Byte order;
    private Boolean isAbbr;
    private NamePart part;
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
     * Gets predicted name's part of token
     *
     * @return value of part: PREFIX, FIRSTNAME, MIDDLENAME, LASTNAME or SUFFIX
     */
    public NamePart getPart() {
        return part;
    }

    public void setPart(NamePart part) {
        this.part = part;
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

    //endregion

    public TokenE(V inV, V outV, String type, String weight) {
        super(inV, outV, type, weight);
        this.order = Byte.valueOf(weight);
        this.isAbbr = StringHelper.isAbbreviated(outV.getVal().length(), inV.getVal(), order);
    }

    @Override
    public String toString() {
        return String.format("E[%s] %s -%s-> %s", super.getType(), super.getInV().getVal(), part, super.getOutV().getVal());
    }
}
