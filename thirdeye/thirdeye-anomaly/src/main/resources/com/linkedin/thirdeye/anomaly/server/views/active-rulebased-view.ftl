<!DOCTYPE html>
<html>
<head>
<style>
table, th, td {
    border: 1px solid black;
    border-collapse: collapse;
}
th, td {
    padding: 5px;
}
</style>
</head>
<body>

<center>
  <h1>All Functions (RuleBased)</h1>
  <b>Database:</b> ${database} <br>
  <b>Table:</b> ${functionTable} <br>
  <b>Collection:</b> ${collection} <br>
  <br>
    <a href="/add">Add a new function.</a>
  <br>
  <br>
  <a href="/active?hideInactive=true">Hide inactive.</a>
  <br>
  <br>
</center>

<table style="width:100%">
  <tr>
    <th>Action</th>
    <th>FunctionId</th>
    <th>FunctionName</th>
    <th>Description</th>
    <th>Metric</th>
    <th>DefaultDelta</th>
    <th>Aggregate</th>
    <th>Baseline</th>
    <th>ConsecutiveBuckets</th>
    <th>CronDefinition</th>
    <th>DeltaTable</th>
  </tr>
  <#list functions as row>
    <tr>
      <td>
        <#if (row.active)>
          <a href="/deactivate/${row.functionId}">deactivate</a>
        <#else>
          <a href="/activate/${row.functionId}">activate</a>
        </#if>
      </td>
      <td>${row.functionId}</td>
      <td>${row.functionName}</td>
      <td>${row.functionDescription}</td>
      <td>${row.metricName}</td>
      <td>${row.delta}</td>
      <td>${row.aggregateSize}-${row.aggregateUnit}</td>
      <td>${row.baselineSize}-${row.baselineUnit}</td>
      <td>${row.consecutiveBuckets}</td>
      <td>${row.cronDefinition!""}</td>
      <td>
        <#if row.deltaTableName??>
          <a href="/rulebased/deltatable/${row.deltaTableName}">${row.deltaTableName}</a>
        <#else>
          N/A
        </#if>
      </td>
    </tr>
  </#list>
</table>

</body>
</html>
