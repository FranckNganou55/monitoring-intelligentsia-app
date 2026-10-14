import { PageIntro, SectionHeading } from '@/components/ui/PageIntro'
import { useEffect, useMemo, useState } from "react";
import {
  AlertTriangle,
  CheckCircle2,
  Power,
  Plus,
  ShieldAlert,
  Search,
  Wifi,
} from "lucide-react";

import { ruleService } from "../../services/ruleService";
import { Modal } from "../../components/ui/Modal";
import {
  MonitoringRule,
  RiskLevel,
  RuleCategory,
} from "../../types/rules";

const riskLevels: RiskLevel[] = [
  "TYPIQUE",
  "MOINS_CRITIQUE",
  "MOYEN",
  "CRITIQUE",
];

const categories: Array<{
  value: RuleCategory;
  label: string;
}> = [
  {
    value: "FRAUD",
    label: "Fraude",
  },
  {
    value: "ATM",
    label: "GAB",
  },
  {
    value: "NETWORK",
    label: "Réseau",
  },
];

const riskLabel: Record<RiskLevel, string> = {
  TYPIQUE: "Typique",
  MOINS_CRITIQUE: "Moins critique",
  MOYEN: "Moyen",
  CRITIQUE: "Critique",
};

const categoryIcon = (category: RuleCategory) => {
  switch (category) {
    case "FRAUD":
      return <ShieldAlert size={18} />;

    case "ATM":
      return <AlertTriangle size={18} />;

    case "NETWORK":
      return <Wifi size={18} />;

    default:
      return null;
  }
};

export default function RulesPage() {
  const [createOpen, setCreateOpen] = useState(false);
  const [creating, setCreating] = useState(false);
  const [savingCategoryId, setSavingCategoryId] = useState<number | null>(null);
  const [createError, setCreateError] = useState<string | null>(null);
  const [createForm, setCreateForm] = useState({
    code: "",
    name: "",
    description: "",
    category: "FRAUD" as RuleCategory,
    riskLevel: "MOYEN" as RiskLevel,
  });
  const [rules, setRules] = useState<MonitoringRule[]>([]);
  const [category, setCategory] =
    useState<RuleCategory | "ALL">("ALL");
  const [searchTerm, setSearchTerm] = useState("");

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [drafts, setDrafts] = useState<Record<number, { riskLevel: RiskLevel }>>({});
  const riskPercentages: Record<RiskLevel, number> = { TYPIQUE: 25, MOINS_CRITIQUE: 50, MOYEN: 75, CRITIQUE: 100 };

  const loadRules = async () => {
    try {
      setLoading(true);
      setError(null);

      const data = await ruleService.getAll();

      setRules(data);
    } catch (err) {
      console.error(err);
      setError(
        "Impossible de charger les règles."
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadRules();
    const reload = () => void loadRules();
    window.addEventListener('monitoring:backend-ready', reload);
    return () => window.removeEventListener('monitoring:backend-ready', reload);
  }, []);

  const filteredRules = useMemo(() => {
    const query = searchTerm.trim().toLocaleLowerCase();
    return rules.filter((rule) => {
      const matchesCategory = category === "ALL" || rule.category === category;
      const matchesSearch = !query || [rule.code, rule.name, rule.description ?? ""]
        .some((value) => value.toLocaleLowerCase().includes(query));
      return matchesCategory && matchesSearch;
    });
  }, [rules, category, searchTerm]);

  const updateDraft = (rule: MonitoringRule, values: Partial<{ riskLevel: RiskLevel }>) => {
    setDrafts((current) => ({
      ...current,
      [rule.id]: {
        riskLevel: current[rule.id]?.riskLevel ?? rule.riskLevel,
        ...values,
      },
    }));
  };

  const saveRiskConfiguration = async (rule: MonitoringRule) => {
    const draft = drafts[rule.id];
    if (!draft) return;
    const riskPercentage = riskPercentages[draft.riskLevel];
    try {
      setError(null);
      const updated = await ruleService.update(rule.id, {
        code: rule.code,
        name: rule.name,
        description: rule.description,
        category: rule.category,
        riskLevel: draft.riskLevel,
        riskPercentage,
        enabled: rule.enabled,
        notificationEnabled: rule.notificationEnabled,
        soundEnabled: rule.soundEnabled,
        visualAlertEnabled: rule.visualAlertEnabled,
      });
      setRules((current) => current.map((item) => item.id === updated.id ? updated : item));
      setDrafts((current) => { const next = { ...current }; delete next[rule.id]; return next; });
    } catch (err) {
      console.error(err);
      setError("Impossible d’enregistrer le niveau et le pourcentage de risque.");
    }
  };

  const createRule = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setCreating(true);
    setCreateError(null);
    try {
      const created = await ruleService.create({
        ...createForm,
        code: createForm.code.trim(),
        name: createForm.name.trim(),
        description: createForm.description.trim() || undefined,
        riskPercentage: riskPercentages[createForm.riskLevel],
        enabled: true,
        notificationEnabled: true,
        soundEnabled: true,
        visualAlertEnabled: true,
      });
      setRules((current) => [...current, created]);
      setCreateOpen(false);
      setCreateForm({ code: "", name: "", description: "", category: "FRAUD", riskLevel: "MOYEN" });
    } catch (err) {
      console.error(err);
      setCreateError("La règle n’a pas pu être ajoutée. Vérifiez les champs puis réessayez.");
    } finally {
      setCreating(false);
    }
  };

  const handleEnabledChange = async (
    rule: MonitoringRule
  ) => {
    try {
      const updated =
        await ruleService.updateEnabled(
          rule.id,
          !rule.enabled
        );

      setRules((current) =>
        current.map((item) =>
          item.id === updated.id
            ? updated
            : item
        )
      );
    } catch (err) {
      console.error(err);
      setError(
        "Impossible de modifier l'état de la règle."
      );
    }
  };

  const handleCategoryChange = async (rule: MonitoringRule, nextCategory: RuleCategory) => {
    if (rule.category === nextCategory) return;
    setSavingCategoryId(rule.id);
    setError(null);
    try {
      const nextRisk = drafts[rule.id]?.riskLevel ?? rule.riskLevel;
      const updated = await ruleService.update(rule.id, {
        code: rule.code,
        name: rule.name,
        description: rule.description,
        category: nextCategory,
        riskLevel: nextRisk,
        riskPercentage: riskPercentages[nextRisk],
        enabled: rule.enabled,
        notificationEnabled: rule.notificationEnabled,
        soundEnabled: rule.soundEnabled,
        visualAlertEnabled: rule.visualAlertEnabled,
      });
      setRules((current) => current.map((item) => item.id === updated.id ? updated : item));
    } catch (err) {
      console.error(err);
      setError(`Impossible de modifier le type d’alerte pour ${rule.code}.`);
    } finally {
      setSavingCategoryId(null);
    }
  };

  return (
    <div className="p-6 space-y-6">

      <div className="page-header flex flex-wrap items-start justify-between gap-4">
        <PageIntro icon={ShieldAlert} eyebrow="Administration" title="Règles de détection" description="Configurez les catégories et les niveaux de risque des règles de surveillance." />
        <button
          type="button"
          onClick={() => { setCreateError(null); setCreateOpen(true); }}
          className="inline-flex items-center gap-2 rounded-lg bg-gold-500 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-gold-600"
        >
          <Plus size={17} />
          Ajouter une règle
        </button>
      </div>

      <div className="grid gap-3 sm:grid-cols-3">
        {[{ label: 'Règles du catalogue', value: rules.length }, { label: 'Règles actives', value: rules.filter(rule => rule.enabled).length }, { label: 'Risque critique', value: rules.filter(rule => rule.riskLevel === 'CRITIQUE').length }].map(item => <div key={item.label} className="panel px-5 py-4"><p className="text-xs font-medium text-gray-500">{item.label}</p><p className="mt-2 text-2xl font-bold text-gray-900">{loading || error ? '—' : item.value}</p></div>)}
      </div>

      <Modal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        title="Ajouter une règle"
        footer={
          <>
            <button type="button" onClick={() => setCreateOpen(false)} className="rounded-lg border px-4 py-2 text-sm font-medium hover:bg-gray-50">Annuler</button>
            <button type="submit" form="create-rule-form" disabled={creating} className="rounded-lg bg-gold-500 px-4 py-2 text-sm font-semibold text-white hover:bg-gold-600 disabled:opacity-50">
              {creating ? "Ajout…" : "Ajouter la règle"}
            </button>
          </>
        }
      >
        <form id="create-rule-form" onSubmit={createRule} className="space-y-4">
          {createError && <p role="alert" className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">{createError}</p>}
          <label className="block space-y-1.5 text-sm font-medium text-gray-700">
            Code de la règle
            <input required maxLength={50} value={createForm.code} onChange={(event) => setCreateForm((form) => ({ ...form, code: event.target.value.toUpperCase() }))} placeholder="Ex. RULE_21" className="w-full rounded-lg border border-gray-300 px-3 py-2.5 font-mono font-normal outline-none focus:border-gold-500 focus:ring-2 focus:ring-gold-100" />
          </label>
          <label className="block space-y-1.5 text-sm font-medium text-gray-700">
            Nom
            <input required maxLength={200} value={createForm.name} onChange={(event) => setCreateForm((form) => ({ ...form, name: event.target.value }))} className="w-full rounded-lg border border-gray-300 px-3 py-2.5 font-normal outline-none focus:border-gold-500 focus:ring-2 focus:ring-gold-100" />
          </label>
          <label className="block space-y-1.5 text-sm font-medium text-gray-700">
            Description
            <textarea rows={3} value={createForm.description} onChange={(event) => setCreateForm((form) => ({ ...form, description: event.target.value }))} className="w-full rounded-lg border border-gray-300 px-3 py-2.5 font-normal outline-none focus:border-gold-500 focus:ring-2 focus:ring-gold-100" />
          </label>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <label className="block space-y-1.5 text-sm font-medium text-gray-700">
              Type
              <select value={createForm.category} onChange={(event) => setCreateForm((form) => ({ ...form, category: event.target.value as RuleCategory }))} className="w-full rounded-lg border border-gray-300 px-3 py-2.5 font-normal">
                {categories.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
              </select>
            </label>
            <label className="block space-y-1.5 text-sm font-medium text-gray-700">
              Niveau de risque
              <select value={createForm.riskLevel} onChange={(event) => setCreateForm((form) => ({ ...form, riskLevel: event.target.value as RiskLevel }))} className="w-full rounded-lg border border-gray-300 px-3 py-2.5 font-normal">
                {riskLevels.map((level) => <option key={level} value={level}>{riskLabel[level]}</option>)}
              </select>
            </label>
          </div>
          <p className="text-xs text-gray-500">La règle sera créée active avec les notifications activées. Le risque Moyen ou Critique active le son; Critique active aussi le signal visuel.</p>
        </form>
      </Modal>

      <div className="panel space-y-4 p-5">
        <SectionHeading title="Catalogue des règles" description={`${filteredRules.length} règle(s) correspondant aux filtres`} />
        <div className="flex flex-wrap items-center gap-3">
        <label className="flex min-w-[260px] flex-1 items-center gap-3 rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 text-gray-400">
          <Search size={18} />
          <input
            value={searchTerm}
            onChange={(event) => setSearchTerm(event.target.value)}
            placeholder="Rechercher un code, un nom ou une description…"
            className="w-full bg-transparent text-sm text-gray-800 outline-none placeholder:text-gray-400"
            aria-label="Rechercher un code, un nom ou une description"
          />
        </label>
      </div>

      <div className="flex gap-2 flex-wrap">

        <button
          onClick={() => setCategory("ALL")}
          className={`px-4 py-2 rounded-lg border ${
            category === "ALL"
              ? "bg-gray-900 text-white"
              : "bg-white"
          }`}
        >
          Toutes
        </button>

        {categories.map((item) => (
          <button
            key={item.value}
            onClick={() =>
              setCategory(item.value)
            }
            className={`px-4 py-2 rounded-lg border flex items-center gap-2 ${
              category === item.value
                ? "bg-gray-900 text-white"
                : "bg-white"
            }`}
          >
            {categoryIcon(item.value)}
            {item.label}
          </button>
        ))}
      </div>
      </div>

      {error && (
        <div className="rounded-lg border border-red-200 bg-red-50 text-red-700 p-4">
          {error}
        </div>
      )}

      {loading ? (
        <div className="p-8 text-center text-gray-500">
          Chargement des règles...
        </div>
      ) : (
        <div className="overflow-x-auto bg-white rounded-xl border">

          <table className="w-full min-w-[1080px] text-sm">

            <thead className="bg-gray-50 border-b">

              <tr>
                <th className="text-left p-4">
                  Règle
                </th>

                <th className="text-left p-4">
                  Type d’alerte
                </th>

                <th className="text-left p-4">
                  Niveau de risque
                </th>

                <th className="text-left p-4">
                  Alertes
                </th>

                <th className="text-left p-4">
                  Risque (%)
                </th>

                <th className="text-left p-4">
                  Part des alertes
                </th>

                <th className="text-left p-4">
                  État
                </th>

                <th className="text-left p-4">
                  Actions
                </th>
              </tr>

            </thead>

            <tbody>

              {filteredRules.map((rule) => (

                <tr
                  key={rule.id}
                  className="border-b last:border-b-0"
                >

                  <td className="p-4">

                    <div className="font-medium">
                      {rule.code}
                    </div>

                    <div className="text-gray-500">
                      {rule.name}
                    </div>

                  </td>

                  <td className="p-4">
                    <div className="flex items-center gap-2">
                      {categoryIcon(rule.category)}
                      <select
                        value={rule.category}
                        disabled={savingCategoryId === rule.id}
                        onChange={(event) => void handleCategoryChange(rule, event.target.value as RuleCategory)}
                        className="rounded-lg border px-3 py-2 disabled:opacity-50"
                        aria-label={`Type d’alerte pour ${rule.code} — ${rule.name}`}
                      >
                        {categories.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
                      </select>
                    </div>
                  </td>

                  <td className="p-4">

                    <select
                      value={drafts[rule.id]?.riskLevel ?? rule.riskLevel}
                      onChange={(event) => updateDraft(rule, { riskLevel: event.target.value as RiskLevel })}
                      className="border rounded-lg px-3 py-2"
                    >

                      {riskLevels.map((level) => (
                        <option
                          key={level}
                          value={level}
                        >
                          {riskLabel[level]}
                        </option>
                      ))}

                    </select>

                  </td>

                  <td className="p-4">
                    {rule.alertCount}
                  </td>

                  <td className="p-4">
                    <div className="flex items-center">
                      <input
                        aria-label={`Pourcentage de risque pour ${rule.code}`}
                        type="number"
                        min="0"
                        max="100"
                        step="0.01"
                        readOnly
                        value={riskPercentages[drafts[rule.id]?.riskLevel ?? rule.riskLevel]}
                        className="w-16 rounded-lg border bg-gray-50 px-2 py-2"
                      />
                      <span className="ml-1">%</span>
                    </div>
                  </td>

                  <td className="whitespace-nowrap p-4 font-semibold">{rule.alertPercentage.toFixed(2)} %</td>

                  <td className="p-4">

                    {rule.enabled ? (
                      <span className="alert-status-badge bg-green-50 text-green-700">
                        <CheckCircle2 size={16} />
                        Active
                      </span>
                    ) : (
                      <span className="text-gray-400">
                        Désactivée
                      </span>
                    )}

                  </td>

                  <td className="p-4">

                    <div className="flex flex-wrap gap-2">
                    {drafts[rule.id] && (
                      <button
                        onClick={() => void saveRiskConfiguration(rule)}
                        className="rounded-lg bg-gold-500 px-3 py-2 font-medium text-white hover:bg-gold-600"
                      >
                        Valider
                      </button>
                    )}

                    <button
                      onClick={() =>
                        handleEnabledChange(rule)
                      }
                      className="inline-flex items-center gap-2 px-3 py-2 border rounded-lg hover:bg-gray-50"
                    >

                      <Power size={16} />

                      {rule.enabled
                        ? "Désactiver"
                        : "Activer"}

                    </button>
                    </div>

                  </td>

                </tr>

              ))}

              {filteredRules.length === 0 && (
                <tr>
                    <td
                    colSpan={8}
                    className="p-8 text-center text-gray-500"
                  >
                    Aucune règle disponible.
                  </td>
                </tr>
              )}

            </tbody>

          </table>

        </div>
      )}

    </div>
  );
}
