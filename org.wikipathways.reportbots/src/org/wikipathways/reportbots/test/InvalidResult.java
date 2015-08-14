package org.wikipathways.reportbots.test;

import java.util.ArrayList;
import java.util.HashSet;

import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.wikipathways.webservice.WSPathwayInfo;
import org.wikipathways.reportbots.OutdatedResult;

public class InvalidResult {
	
	WSPathwayInfo pwInfo;	
	PathwayElement pe;
	int nbInvalid;
	
	public InvalidResult(WSPathwayInfo pwInfo,PathwayElement pe,int nbInvalid) {	
		this.pwInfo = pwInfo;
		this.pe = pe;
		this.nbInvalid = nbInvalid;
	}

	public WSPathwayInfo getPwInfo() {
		return pwInfo;
	}

	public void setPwInfo(WSPathwayInfo pwInfo) {
		this.pwInfo = pwInfo;
	}

	public int getNbInvalid() {
		return nbInvalid;
	}

	public void setNbInvalid(int nbInvalid) {
		this.nbInvalid = nbInvalid;
	}

	public PathwayElement getPe() {
		return pe;
	}

	public void setPe(PathwayElement pe) {
		this.pe = pe;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((pe.getXref().getId() == null) ? 0 : pe.getXref().getId().hashCode());
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
		InvalidResult other = (InvalidResult) obj;
		if (pe.getXref().getId() == null) {
			if (other.pe.getXref().getId() != null)
				return false;
		} else if (!pe.getXref().getId().equals(other.pe.getXref().getId()))
			return false;
		if (pwInfo.getId() == null) {
			if (other.pwInfo.getId() != null)
				return false;
		} else if (!pwInfo.getId().equals(other.pwInfo.getId()))
			return false;
		return true;
	}	
}
