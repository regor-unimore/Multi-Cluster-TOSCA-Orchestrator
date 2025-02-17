package it.unict.vertx.esb.packet.create;

public interface CreateDB extends Create {
	
	public enum Status {
		// Stati relativi alla creazione del DB
		OK,
		ERROR,
		UNRECOGNIZED;
		
		public static Status forValue(String value) {
			if (value != null) {
				for (Status s : Status.values()) {
					if (s.name().equalsIgnoreCase(value))
						return s;
				}
			}
			return Status.UNRECOGNIZED;
		}
		
		public String value() {
	        return name().toLowerCase();
		}
	 
	}

}
