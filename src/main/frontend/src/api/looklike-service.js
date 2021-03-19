import { config } from "../configs/apiConfigs";
import axios from "axios";

export const lookLikeService = {
  getLookALike,
  getCollections
};

function getLookALike(snapShot) {
  const callUrl = `${config.apiUrl}/similar`;
  
  return axios
    .post(callUrl, snapShot, { headers: { "Content-Type": "application/json" } })
    .then(response => {
      return response.data;
    })
    .catch(error => {
      return Promise.reject(error);
    });
}

function getCollections() {
  const callUrl = `${config.apiUrl}/collections`;
  return axios.get(callUrl).then(response => {
    return response.data
  }).catch(error => {
    return Promise.reject(error)
  })

}

