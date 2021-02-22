import { config } from "../configs/apiConfigs";
import axios from "axios";

export const lookLikeService = {
  getLookALike
};

function getLookALike(user) {
  const callUrl = `${config.apiUrl}/similar`;
  return axios
    .post(callUrl, user, { headers: { "Content-Type": "application/json" } })
    .then(response => {
      console.log(response);
      return response.data;
    })
    .catch(error => {
      return Promise.reject(error);
    });
}
