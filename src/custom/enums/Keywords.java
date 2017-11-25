package custom.enums;

/**
 * This Enum gives a rapid access to the levels of privileges that a user may
 * have
 * 
 * @author paolobruzzo
 */
public enum Keywords {

	/* User attributes when not logged */
	NOT_LOGGED_USER(null),
	LOGGED_OUT_USER(""),
	
	/* Keywords used in the policy file as parameters*/
	USER_ID_ATTRIBUTE("current_user_id"),
	USER_ROLE_ATTRIBUTE("current_user_role"),
	
	/* Date format*/
	DATE_FORMAT("yyyy-MM-dd HH:mm:ss"),
	
	/* Aggregates */
	COUNT("COUNT"),
	SUM("SUM"),
	AVG("AVG"),
	MIN("MIN"),
	MAX("MAX"),
	
	/* Keywords used in the policy file to describe the entities*/
	TABLE("table"),
	TABLE_NAME("name"),
	TABLE_COLUMNS("columns"),
	RELATION_VIEW("relation_view"),
	USER_ROLE("user_role"),
	BEGIN_DATE("begin_date"),
	EXPIRATION_DATE("expiration"),
	PERMISSION("permission"),
	POLICY("policy"),
	VALUES_SEPARATOR(",");
	;

	private final String level;

	Keywords(String level) {
		this.level = level;
	}
	
	/**
	 * @return the String associated to the keyword
	 */
	public String toString() {
		return level;
	}
}
