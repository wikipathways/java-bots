// PathVisio,
// a tool for data visualization and analysis using Biological Pathways
// Copyright 2006-2015 BiGCaT Bioinformatics
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package org.wikipathways.reportbots;


public class OutdatedResult {
	private String pwID;	
	private String pwName;
	private String refLabel;
	private String refID;
	
	public OutdatedResult(String pwID, String pwName, String refLabel,
			String refID) {
		this.pwID = pwID;
		this.pwName = pwName;
		this.refLabel = refLabel;
		this.refID = refID;
	}

	public String getPwID() {
		return pwID;
	}

	public void setPwID(String pwID) {
		this.pwID = pwID;
	}

	public String getPwName() {
		return pwName;
	}

	public void setPwName(String pwName) {
		this.pwName = pwName;
	}

	public String getRefLabel() {
		return refLabel;
	}

	public void setRefLabel(String refLabel) {
		this.refLabel = refLabel;
	}

	public String getRefID() {
		return refID;
	}

	public void setRefID(String refID) {
		this.refID = refID;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((refID == null) ? 0 : refID.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
//		if (this == obj)
//			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OutdatedResult other = (OutdatedResult) obj;
		if (refID == null) {
			if (other.refID != null)
				return false;
		} else if (!refID.equals(other.refID))
			return false;
		if (pwID == null) {
			if (other.pwID != null)
				return false;
		} else if (!pwID.equals(other.pwID))
			return false;
		if (refLabel == null) {
			if (other.refLabel != null)
				return false;
		} else if (!refLabel.equals(other.refLabel))
			return false;
		return true;
	}
	
}
