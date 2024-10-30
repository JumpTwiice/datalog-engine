package ast;

public class Term {
    public Long value;
    public boolean isVar;

    public Term(Long value, boolean isVar) {
        this.value = value;
        this.isVar = isVar;
    }

}
