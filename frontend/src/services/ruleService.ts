import api from "./api";
import {
  MonitoringRule,
  MonitoringRuleRequest,
  RuleCategory,
  RiskLevel,
} from "../types/rules";

const BASE_URL = "/monitoring/rules";

export const ruleService = {

  async getAll(): Promise<MonitoringRule[]> {
    const response = await api.get<MonitoringRule[]>(BASE_URL);
    return response.data;
  },

  async getByCategory(
    category: RuleCategory
  ): Promise<MonitoringRule[]> {
    const response = await api.get<MonitoringRule[]>(
      BASE_URL,
      {
        params: { category },
      }
    );

    return response.data;
  },

  async getById(id: number): Promise<MonitoringRule> {
    const response = await api.get<MonitoringRule>(
      `${BASE_URL}/${id}`
    );

    return response.data;
  },

  async create(
    request: MonitoringRuleRequest
  ): Promise<MonitoringRule> {
    const response = await api.post<MonitoringRule>(
      BASE_URL,
      request
    );

    return response.data;
  },

  async update(
    id: number,
    request: MonitoringRuleRequest
  ): Promise<MonitoringRule> {
    const response = await api.put<MonitoringRule>(
      `${BASE_URL}/${id}`,
      request
    );

    return response.data;
  },

  async updateRiskLevel(
    id: number,
    riskLevel: RiskLevel
  ): Promise<MonitoringRule> {
    const response = await api.patch<MonitoringRule>(
      `${BASE_URL}/${id}/risk-level`,
      {
        riskLevel,
      }
    );

    return response.data;
  },

  async updateEnabled(
    id: number,
    enabled: boolean
  ): Promise<MonitoringRule> {
    const response = await api.patch<MonitoringRule>(
      `${BASE_URL}/${id}/enabled`,
      {
        enabled,
      }
    );

    return response.data;
  },

  async delete(id: number): Promise<void> {
    await api.delete(`${BASE_URL}/${id}`);
  },
};
