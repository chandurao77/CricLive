{{/*
Expand the name of the chart.
*/}}
{{- define "match-service.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "match-service.fullname" -}}
{{- printf "%s-%s" .Release.Name (include "match-service.name" .) | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "match-service.labels" -}}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{ include "match-service.selectorLabels" . }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "match-service.selectorLabels" -}}
app.kubernetes.io/name: {{ include "match-service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}
